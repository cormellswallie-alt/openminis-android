package com.openminis.app.provider

import com.openminis.app.data.model.AgentContentPart
import com.openminis.app.data.model.LLMMessage
import com.openminis.app.data.model.LLMModel
import com.openminis.app.data.model.LLMStreamChunk
import com.openminis.app.data.model.LLMUsage
import com.openminis.app.provider.anthropic.AnthropicProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for the Anthropic prompt-cache / token-accounting fixes:
 *  - [T-anthropic-ctx-cache-undercount]
 *  - [T-anthropic-delta-usage-merge]
 *  - [T-anthropic-cache-breakpoints]
 *  - [T-anthropic-empty-text-block]
 */
class AnthropicCacheAndUsageTest {
    private lateinit var server: MockWebServer
    private lateinit var provider: AnthropicProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        provider = AnthropicProvider(
            apiKey = "test-key",
            model = LLMModel.claudeHaiku45,
            basePath = server.url("/").toString().trimEnd('/'),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun okEmpty() = MockResponse().setBody(
        """{"content":[{"type":"text","text":"ok"}],"stop_reason":"end_turn","usage":{"input_tokens":1,"output_tokens":1}}""",
    )

    private fun sentBody(): JSONObject = JSONObject(server.takeRequest().body.readUtf8())

    private fun markedBlocks(messages: JSONArray): List<Pair<Int, JSONObject>> {
        val out = mutableListOf<Pair<Int, JSONObject>>()
        for (i in 0 until messages.length()) {
            val c = messages.getJSONObject(i).opt("content")
            if (c is JSONArray) for (k in 0 until c.length()) {
                val b = c.getJSONObject(k)
                if (b.has("cache_control")) out.add(i to b)
            }
        }
        return out
    }

    // ── usage ────────────────────────────────────────────────────────────

    @Test
    fun `context size includes cached tokens`() {
        val u = provider.parseUsage(
            JSONObject("""{"input_tokens":12,"output_tokens":3,"cache_read_input_tokens":148000,"cache_creation_input_tokens":900}"""),
        )
        assertEquals(12, u.inputTokens)
        assertEquals(148000, u.cacheReadInputTokens)
        assertEquals(900, u.cacheCreationInputTokens)
        assertEquals(12 + 148000 + 900, u.latestContextTokens)
    }

    @Test
    fun `output-only delta keeps start usage`() {
        val start = LLMUsage(10, 1, cacheCreationInputTokens = 200, cacheReadInputTokens = 5000, latestContextTokens = 5210)
        val delta = provider.parseUsage(JSONObject("""{"output_tokens":420}"""))
        assertEquals(0, delta.latestContextTokens)
        val merged = provider.mergeDeltaUsage(start, delta)
        assertEquals(10, merged.inputTokens)
        assertEquals(420, merged.outputTokens)
        assertEquals(5000, merged.cacheReadInputTokens)
        assertEquals(200, merged.cacheCreationInputTokens)
        assertEquals(5210, merged.latestContextTokens)
    }

    @Test
    fun `full delta overrides start usage`() {
        val start = LLMUsage(10, 1, cacheReadInputTokens = 5000, latestContextTokens = 5010)
        val delta = provider.parseUsage(JSONObject("""{"input_tokens":10,"output_tokens":99,"cache_read_input_tokens":5000}"""))
        val merged = provider.mergeDeltaUsage(start, delta)
        assertEquals(99, merged.outputTokens)
        assertEquals(5010, merged.latestContextTokens)
    }

    @Test
    fun `stream usage survives relay output-only message_delta`() = runBlocking {
        val sse = listOf(
            """{"type":"message_start","message":{"usage":{"input_tokens":7,"output_tokens":1,"cache_read_input_tokens":9000}}}""",
            """{"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""",
            """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"hi"}}""",
            """{"type":"content_block_stop","index":0}""",
            """{"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":55}}""",
            """{"type":"message_stop"}""",
        ).joinToString("") { "data: $it\n\n" }
        server.enqueue(MockResponse().setBody(sse).setHeader("Content-Type", "text/event-stream"))
        val chunks = provider.streamMessage(listOf(LLMMessage(LLMMessage.Role.USER, "x")), null, 256).toList()
        val last = chunks.filterIsInstance<LLMStreamChunk.Usage>().last().usage
        assertEquals(55, last.outputTokens)
        assertEquals(9000, last.cacheReadInputTokens)
        assertEquals(9007, last.latestContextTokens)
    }

    // ── cache breakpoints ────────────────────────────────────────────────

    @Test
    fun `plain string user turns get message breakpoints`() = runBlocking {
        server.enqueue(okEmpty())
        val history = listOf(
            LLMMessage(LLMMessage.Role.USER, "first question"),
            LLMMessage(LLMMessage.Role.ASSISTANT, "first answer"),
            LLMMessage(LLMMessage.Role.USER, "second question"),
            LLMMessage(LLMMessage.Role.ASSISTANT, "second answer"),
            LLMMessage(LLMMessage.Role.USER, "third question"),
        )
        provider.sendMessage(history, "sys", 256)
        val msgs = sentBody().getJSONArray("messages")
        val marked = markedBlocks(msgs)
        assertEquals("two message-level breakpoints expected", 2, marked.size)
        assertEquals(listOf(2, 4), marked.map { it.first })
        assertEquals("third question", marked.last().second.getString("text"))
    }

    @Test
    fun `total breakpoints never exceed four`() = runBlocking {
        server.enqueue(okEmpty())
        val tool = com.openminis.app.data.model.AgentToolDefinition(
            name = "t", description = "d", parameters = emptyMap(), required = emptyList(),
        )
        val history = (1..6).flatMap {
            listOf(LLMMessage(LLMMessage.Role.USER, "u$it"), LLMMessage(LLMMessage.Role.ASSISTANT, "a$it"))
        } + LLMMessage(LLMMessage.Role.USER, "last")
        provider.sendMessage(history, "sys", 256, tools = listOf(tool))
        val raw = server.takeRequest().body.readUtf8()
        val count = Regex("\"cache_control\"").findAll(raw).count()
        assertEquals(4, count)
    }

    @Test
    fun `breakpoint skips trailing empty text`() = runBlocking {
        server.enqueue(okEmpty())
        val history = listOf(
            LLMMessage(
                LLMMessage.Role.USER, "",
                contentParts = listOf(AgentContentPart.Text("real"), AgentContentPart.Text("")),
            ),
        )
        provider.sendMessage(history, null, 256)
        val content = sentBody().getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        assertEquals("empty text part dropped", 1, content.length())
        assertTrue(content.getJSONObject(0).has("cache_control"))
    }

    // ── empty blocks ─────────────────────────────────────────────────────

    @Test
    fun `empty tool output is not an empty text block`() = runBlocking {
        server.enqueue(okEmpty())
        val history = listOf(
            LLMMessage(LLMMessage.Role.USER, "run it"),
            LLMMessage(
                LLMMessage.Role.ASSISTANT, "",
                contentParts = listOf(AgentContentPart.ToolUse("tu_1", "shell_execute", JSONObject().put("command", "true"))),
            ),
            LLMMessage(
                LLMMessage.Role.USER, "",
                contentParts = listOf(AgentContentPart.ToolResult(id = "tu_1", name = "shell_execute", content = "")),
            ),
        )
        provider.sendMessage(history, null, 256)
        val raw = server.takeRequest().body.readUtf8()
        assertFalse("no empty text block on the wire", raw.contains("\"text\":\"\""))
    }

    @Test
    fun `message emptied by dropped parts is skipped`() = runBlocking {
        server.enqueue(okEmpty())
        val history = listOf(
            LLMMessage(LLMMessage.Role.USER, "hello"),
            LLMMessage(LLMMessage.Role.ASSISTANT, "", contentParts = listOf(AgentContentPart.Text(""))),
            LLMMessage(LLMMessage.Role.USER, "again"),
        )
        provider.sendMessage(history, null, 256)
        val msgs = sentBody().getJSONArray("messages")
        for (i in 0 until msgs.length()) {
            val c = msgs.getJSONObject(i).get("content")
            if (c is JSONArray) assertTrue("no empty content array at $i", c.length() > 0)
        }
        // Roles still alternate (the two user turns merged).
        assertEquals(1, msgs.length())
        assertEquals("user", msgs.getJSONObject(0).getString("role"))
    }
}

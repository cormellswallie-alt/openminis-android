package com.openminis.app.provider

import com.openminis.app.auth.ClaudeOAuthManager
import com.openminis.app.data.model.AgentContentPart
import com.openminis.app.data.model.LLMError
import com.openminis.app.data.model.LLMMessage
import com.openminis.app.data.model.LLMModel
import com.openminis.app.data.model.LLMStreamChunk
import com.openminis.app.data.model.ThinkingLevel
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Anthropic protocol compatibility, checked against platform.claude.com docs:
 *  - [T-anthropic-apikey-prefix-throw]  API-key / compatible providers must work
 *    on builds without ANTHROPIC_OAUTH_IDENTIFIER_PROMPT.
 *  - [T-anthropic-signed-thinking]       signed thinking blocks round-trip.
 *  - [T-anthropic-wire-compat]           400 → retry without optional features.
 *  - [T-anthropic-budget-floor]          budget_tokens >= 1024.
 *  - [T-anthropic-no-prefill]            request never ends on an assistant turn.
 */
class AnthropicCompatTest {
    private lateinit var server: MockWebServer
    private val originalPrefixSource = ClaudeOAuthManager.identifierPromptSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        ClaudeOAuthManager.identifierPromptSource = originalPrefixSource
        server.shutdown()
    }

    private fun base() = server.url("/").toString().trimEnd('/')

    private fun provider(model: LLMModel, isOAuth: Boolean = false) = AnthropicProvider(
        apiKey = "test-key",
        model = model,
        basePath = base(),
        isOAuth = isOAuth,
    )

    private fun ok() = MockResponse().setHeader("Content-Type", "application/json").setBody(
        """{"content":[{"type":"text","text":"ok"}],"stop_reason":"end_turn","usage":{"input_tokens":1,"output_tokens":1}}""",
    )

    private fun sent(): JSONObject = JSONObject(server.takeRequest().body.readUtf8())

    private fun sse(vararg events: String, compact: Boolean = false) =
        events.joinToString("") { if (compact) "data:$it\n\n" else "data: $it\n\n" }

    // ── identifier prompt ───────────────────────────────────────────────

    @Test
    fun `api key provider works when the oauth identifier prompt is not configured`() = runBlocking {
        ClaudeOAuthManager.identifierPromptSource = { "" }
        server.enqueue(ok())
        val resp = provider(LLMModel.claudeOpus55).sendMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 128,
        )
        assertEquals("ok", resp.text)
        assertFalse("no system block for a null prompt", sent().has("system"))
    }

    @Test
    fun `api key provider keeps its own system prompt without any oauth prefix`() = runBlocking {
        ClaudeOAuthManager.identifierPromptSource = { "" }
        server.enqueue(ok())
        provider(LLMModel.claudeSonnet46).sendMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "hi")), "be brief", 256,
        )
        val system = sent().getJSONArray("system")
        assertEquals(1, system.length())
        assertEquals("be brief", system.getJSONObject(0).getString("text"))
    }

    @Test
    fun `oauth without a configured prefix fails with a clear error instead of sending`() = runBlocking {
        ClaudeOAuthManager.identifierPromptSource = { "" }
        try {
            provider(LLMModel.claudeSonnet46, isOAuth = true).sendMessage(
                listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 256,
            )
            fail("OAuth must not go out without the Claude Code prefix")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("ANTHROPIC_OAUTH_IDENTIFIER_PROMPT"))
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `configured prefix is the non-throwing read`() {
        ClaudeOAuthManager.identifierPromptSource = { "" }
        assertEquals("", ClaudeOAuthManager.configuredIdentifierPrompt)
        ClaudeOAuthManager.identifierPromptSource = { "PFX" }
        assertEquals("PFX", ClaudeOAuthManager.configuredIdentifierPrompt)
        assertEquals("PFX", ClaudeOAuthManager.ANTHROPIC_OAUTH_IDENTIFIER_PROMPT)
    }

    // ── signed thinking ─────────────────────────────────────────────────

    @Test
    fun `stream captures signed thinking blocks on Finished`() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/event-stream").setBody(
                sse(
                    """{"type":"message_start","message":{"usage":{"input_tokens":5,"output_tokens":1}}}""",
                    """{"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":"","signature":""}}""",
                    """{"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"Let me "}}""",
                    """{"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"check."}}""",
                    """{"type":"content_block_delta","index":0,"delta":{"type":"signature_delta","signature":"SIG123"}}""",
                    """{"type":"content_block_stop","index":0}""",
                    """{"type":"content_block_start","index":1,"content_block":{"type":"redacted_thinking","data":"ENC"}}""",
                    """{"type":"content_block_stop","index":1}""",
                    """{"type":"content_block_start","index":2,"content_block":{"type":"tool_use","id":"toolu_1","name":"shell_execute","input":{}}}""",
                    """{"type":"content_block_delta","index":2,"delta":{"type":"input_json_delta","partial_json":"{\"command\":\"ls\"}"}}""",
                    """{"type":"content_block_stop","index":2}""",
                    """{"type":"message_delta","delta":{"stop_reason":"tool_use"},"usage":{"output_tokens":20}}""",
                    """{"type":"message_stop"}""",
                ),
            ),
        )
        val chunks = provider(LLMModel.claudeSonnet46).streamMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "x")), null, 4096, thinkingLevel = ThinkingLevel.HIGH,
        ).toList()
        val finished = chunks.filterIsInstance<LLMStreamChunk.Finished>().single()
        assertEquals("tool_use", finished.stopReason)
        val blocks = JSONArray(finished.anthropicThinkingBlocks!!)
        assertEquals(2, blocks.length())
        assertEquals("thinking", blocks.getJSONObject(0).getString("type"))
        assertEquals("Let me check.", blocks.getJSONObject(0).getString("thinking"))
        assertEquals("SIG123", blocks.getJSONObject(0).getString("signature"))
        assertEquals("redacted_thinking", blocks.getJSONObject(1).getString("type"))
        assertEquals("ENC", blocks.getJSONObject(1).getString("data"))
    }

    @Test
    fun `compact data lines without a space are parsed`() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/event-stream").setBody(
                sse(
                    """{"type":"message_start","message":{"usage":{"input_tokens":5,"output_tokens":1}}}""",
                    """{"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""",
                    """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"hello"}}""",
                    """{"type":"content_block_stop","index":0}""",
                    """{"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":2}}""",
                    compact = true,
                ),
            ),
        )
        val chunks = provider(LLMModel.claudeHaiku45).streamMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "x")), null, 256,
        ).toList()
        assertEquals("hello", chunks.filterIsInstance<LLMStreamChunk.Text>().joinToString("") { it.text })
    }

    private fun toolLoopHistory(blocks: String?): List<LLMMessage> = listOf(
        LLMMessage(LLMMessage.Role.USER, "list files"),
        LLMMessage(
            role = LLMMessage.Role.ASSISTANT,
            content = "Checking.",
            contentParts = listOf(
                AgentContentPart.Text("Checking."),
                AgentContentPart.ToolUse("toolu_1", "shell_execute", JSONObject().put("command", "ls")),
            ),
            anthropicThinkingBlocks = blocks,
        ),
        LLMMessage(
            role = LLMMessage.Role.USER,
            content = "",
            contentParts = listOf(AgentContentPart.ToolResult("toolu_1", "shell_execute", "a.txt")),
        ),
    )

    private val signedBlocks =
        """[{"type":"thinking","thinking":"Let me check.","signature":"SIG123"},{"type":"redacted_thinking","data":"ENC"}]"""

    private fun assistantContent(body: JSONObject): JSONArray {
        val msgs = body.getJSONArray("messages")
        for (i in 0 until msgs.length()) {
            val m = msgs.getJSONObject(i)
            if (m.getString("role") == "assistant") return m.getJSONArray("content")
        }
        error("no assistant message")
    }

    @Test
    fun `signed thinking is replayed first and unchanged in the tool loop`() = runBlocking {
        server.enqueue(ok())
        provider(LLMModel.claudeSonnet46).sendMessage(
            toolLoopHistory(signedBlocks), null, 4096, thinkingLevel = ThinkingLevel.HIGH,
        )
        val content = assistantContent(sent())
        assertEquals("thinking", content.getJSONObject(0).getString("type"))
        assertEquals("SIG123", content.getJSONObject(0).getString("signature"))
        assertEquals("Let me check.", content.getJSONObject(0).getString("thinking"))
        assertFalse(content.getJSONObject(0).has("cache_control"))
        assertEquals("redacted_thinking", content.getJSONObject(1).getString("type"))
        assertEquals("text", content.getJSONObject(2).getString("type"))
        assertEquals("tool_use", content.getJSONObject(3).getString("type"))
    }

    @Test
    fun `legacy budget model also replays signed thinking`() = runBlocking {
        server.enqueue(ok())
        provider(LLMModel.claudeHaiku45).sendMessage(
            toolLoopHistory(signedBlocks), null, 16_000, thinkingLevel = ThinkingLevel.LOW,
        )
        val body = sent()
        assertEquals("enabled", body.getJSONObject("thinking").getString("type"))
        assertEquals("thinking", assistantContent(body).getJSONObject(0).getString("type"))
    }

    @Test
    fun `no replay when thinking is off or blocks are unsigned`() = runBlocking {
        server.enqueue(ok())
        provider(LLMModel.claudeOpus48).sendMessage(
            toolLoopHistory(signedBlocks), null, 4096, thinkingLevel = ThinkingLevel.OFF,
        )
        val off = assistantContent(sent())
        for (i in 0 until off.length()) {
            assertFalse(off.getJSONObject(i).getString("type").contains("thinking"))
        }
        server.enqueue(ok())
        provider(LLMModel.claudeSonnet46).sendMessage(
            toolLoopHistory("""[{"type":"thinking","thinking":"x","signature":""}]"""), null, 4096,
            thinkingLevel = ThinkingLevel.HIGH,
        )
        val unsigned = assistantContent(sent())
        assertEquals("text", unsigned.getJSONObject(0).getString("type"))
    }

    @Test
    fun `signature 400 retries once without replay and stays off`() = runBlocking {
        val p = provider(LLMModel.claudeSonnet46)
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"type":"error","error":{"type":"invalid_request_error","message":"messages.1.content.0: Invalid `signature` in `thinking` block."}}""",
            ),
        )
        server.enqueue(ok())
        server.enqueue(ok())
        val resp = p.sendMessage(toolLoopHistory(signedBlocks), null, 4096, thinkingLevel = ThinkingLevel.HIGH)
        assertEquals("ok", resp.text)
        assertEquals("thinking", assistantContent(sent()).getJSONObject(0).getString("type"))
        assertEquals("text", assistantContent(sent()).getJSONObject(0).getString("type"))
        assertTrue(AnthropicProvider.WireFeature.SIGNED_THINKING_ECHO in p.disabledFeatures)
        p.sendMessage(toolLoopHistory(signedBlocks), null, 4096, thinkingLevel = ThinkingLevel.HIGH)
        assertEquals("text", assistantContent(sent()).getJSONObject(0).getString("type"))
    }

    @Test
    fun `explicit off rejected by a relay falls back to low effort`() = runBlocking {
        val p = provider(LLMModel.claudeSonnet5)
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"type":"error","error":{"type":"invalid_request_error","message":"\"thinking.type.disabled\" is not supported for this model."}}""",
            ),
        )
        server.enqueue(ok())
        p.sendMessage(listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 256)
        assertEquals("disabled", sent().getJSONObject("thinking").getString("type"))
        val retry = sent()
        assertFalse(retry.has("thinking"))
        assertEquals("low", retry.getJSONObject("output_config").getString("effort"))
        assertTrue(AnthropicProvider.WireFeature.EXPLICIT_THINKING_OFF in p.disabledFeatures)
    }

    @Test
    fun `unrelated 400 without optional features is not retried`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":{"type":"invalid_request_error","message":"bad"}}"""))
        try {
            provider(LLMModel.claudeHaiku45).sendMessage(listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 256)
            fail("expected error")
        } catch (e: LLMError) {
            assertTrue(e.message!!.contains("bad"))
        }
        assertEquals(1, server.requestCount)
    }

    // ── thinking configuration ──────────────────────────────────────────

    @Test
    fun `tiny max tokens never sends a budget below 1024`() = runBlocking {
        server.enqueue(ok())
        provider(LLMModel.claudeHaiku45).sendMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 128, thinkingLevel = ThinkingLevel.HIGH,
        )
        val body = sent()
        assertFalse("budget would be < 1024: $body", body.has("thinking"))
        assertEquals(0, AnthropicProvider.thinkingBudget(1024, ThinkingLevel.LOW))
        assertEquals(1024, AnthropicProvider.thinkingBudget(1025, ThinkingLevel.LOW))
    }

    @Test
    fun `xhigh reaches the wire on 4_7+ and maps to max on 4_6`() = runBlocking {
        server.enqueue(ok())
        provider(LLMModel.claudeOpus48).sendMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 4096, thinkingLevel = ThinkingLevel.XHIGH,
        )
        assertEquals("xhigh", sent().getJSONObject("output_config").getString("effort"))
        server.enqueue(ok())
        provider(LLMModel.claudeSonnet46).sendMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 4096, thinkingLevel = ThinkingLevel.XHIGH,
        )
        assertEquals("max", sent().getJSONObject("output_config").getString("effort"))
    }

    @Test
    fun `thinking off is encoded per model`() {
        assertEquals(AnthropicProvider.Companion.ThinkingOffMode.OMIT, AnthropicProvider.thinkingOffMode("claude-haiku-4-5"))
        assertEquals(AnthropicProvider.Companion.ThinkingOffMode.DISABLED, AnthropicProvider.thinkingOffMode("claude-opus-4-8"))
        assertEquals(AnthropicProvider.Companion.ThinkingOffMode.DISABLED_5X, AnthropicProvider.thinkingOffMode("claude-opus-5"))
        assertEquals(AnthropicProvider.Companion.ThinkingOffMode.DISABLED_5X, AnthropicProvider.thinkingOffMode("claude-sonnet-5"))
        assertEquals(AnthropicProvider.Companion.ThinkingOffMode.DISABLED_5X, AnthropicProvider.thinkingOffMode("claude-haiku-5-5"))
        assertEquals(AnthropicProvider.Companion.ThinkingOffMode.BETWEEN_TOOLS, AnthropicProvider.thinkingOffMode("claude-sonnet-5-5"))
        for (id in listOf("claude-opus-5-5", "claude-fable-5", "claude-fable-5-1", "claude-mythos-5-1")) {
            assertEquals(id, AnthropicProvider.Companion.ThinkingOffMode.LOW_EFFORT, AnthropicProvider.thinkingOffMode(id))
        }
        assertEquals(AnthropicProvider.Companion.ThinkingOffMode.OMIT, AnthropicProvider.thinkingOffMode("deepseek-v4"))
    }

    // ── message shape ───────────────────────────────────────────────────

    @Test
    fun `request never ends on an assistant turn`() = runBlocking {
        server.enqueue(ok())
        provider(LLMModel.claudeOpus55).sendMessage(
            listOf(
                LLMMessage(LLMMessage.Role.USER, "hi"),
                LLMMessage(LLMMessage.Role.ASSISTANT, "partial answer"),
            ),
            null, 256,
        )
        val msgs = sent().getJSONArray("messages")
        assertEquals("user", msgs.getJSONObject(msgs.length() - 1).getString("role"))
    }

    @Test
    fun `auth errors keep the server message`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody(
                """{"type":"error","error":{"type":"permission_error","message":"group quota exhausted"}}""",
            ),
        )
        try {
            provider(LLMModel.claudeHaiku45).sendMessage(listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 256)
            fail("expected error")
        } catch (e: LLMError.InvalidApiKey) {
            assertTrue(e.message!!.contains("group quota exhausted"))
        }
    }

    // ── preserved-thinking prefix mismatch is per request ───────────────

    @Test
    fun `prefix mismatch drops the echo for one request only`() = runBlocking {
        val p = provider(LLMModel.claudeSonnet46)
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"type":"error","error":{"type":"invalid_request_error","message":"messages.1.content.0: Invalid `signature` in `thinking` block. The block is bound to a different conversation. Remove the block, or set `thinking.block_binding.prefix_mismatch_behavior` to drop_block."}}""",
            ),
        )
        server.enqueue(ok())
        server.enqueue(ok())
        p.sendMessage(toolLoopHistory(signedBlocks), null, 4096, thinkingLevel = ThinkingLevel.HIGH)
        assertEquals("thinking", assistantContent(sent()).getJSONObject(0).getString("type"))
        assertEquals("text", assistantContent(sent()).getJSONObject(0).getString("type"))
        assertFalse(AnthropicProvider.WireFeature.SIGNED_THINKING_ECHO in p.disabledFeatures)
        p.sendMessage(toolLoopHistory(signedBlocks), null, 4096, thinkingLevel = ThinkingLevel.HIGH)
        assertEquals("next request replays again", "thinking", assistantContent(sent()).getJSONObject(0).getString("type"))
    }

    @Test
    fun `block binding is never sent to a custom endpoint`() = runBlocking {
        server.enqueue(ok())
        provider(LLMModel.claudeOpus48).sendMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 4096, thinkingLevel = ThinkingLevel.HIGH,
        )
        val req = server.takeRequest()
        val body = JSONObject(req.body.readUtf8())
        assertFalse(body.getJSONObject("thinking").has("block_binding"))
        assertFalse((req.getHeader("anthropic-beta") ?: "").contains("thinking-binding"))
    }

    // ── beta headers ─────────────────────────────────────────────────────

    @Test
    fun `api key requests carry no GA beta values`() = runBlocking {
        val p = provider(LLMModel.claudeOpus48)
        p.cacheMode = AnthropicProvider.CacheTtlMode.EXTENDED
        server.enqueue(ok())
        p.sendMessage(listOf(LLMMessage(LLMMessage.Role.USER, "hi")), "sys", 4096, thinkingLevel = ThinkingLevel.HIGH)
        val beta = server.takeRequest().getHeader("anthropic-beta") ?: ""
        assertFalse(beta, beta.contains("effort-2025-11-24"))
        assertFalse(beta, beta.contains("extended-cache-ttl"))
        // Legacy manual thinking still needs interleaved thinking.
        server.enqueue(ok())
        provider(LLMModel.claudeHaiku45).sendMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 16_000, thinkingLevel = ThinkingLevel.LOW,
        )
        assertTrue((server.takeRequest().getHeader("anthropic-beta") ?: "").contains("interleaved-thinking"))
    }

    // ── cache TTL modes ──────────────────────────────────────────────────

    private fun userTurns(n: Int): List<LLMMessage> = buildList {
        for (i in 1..n) {
            add(LLMMessage(LLMMessage.Role.USER, "question $i"))
            if (i < n) add(LLMMessage(LLMMessage.Role.ASSISTANT, "answer $i"))
        }
    }

    /** (message index, ttl or "5m") for every message-level breakpoint. */
    private fun messageMarks(body: JSONObject): List<Pair<Int, String>> {
        val out = mutableListOf<Pair<Int, String>>()
        val msgs = body.getJSONArray("messages")
        for (i in 0 until msgs.length()) {
            val c = msgs.getJSONObject(i).opt("content") as? JSONArray ?: continue
            for (k in 0 until c.length()) {
                c.getJSONObject(k).optJSONObject("cache_control")?.let { out.add(i to it.optString("ttl", "5m")) }
            }
        }
        return out
    }

    private fun bodyFor(mode: AnthropicProvider.CacheTtlMode, history: List<LLMMessage>, longTail: Boolean = false): JSONObject {
        val p = provider(LLMModel.claudeOpus48)
        p.cacheMode = mode
        p.cacheLongTail = longTail
        server.enqueue(ok())
        runBlocking { p.sendMessage(history, "sys", 4096) }
        return sent()
    }

    @Test
    fun `standard and extended mark the last two user turns`() {
        val std = bodyFor(AnthropicProvider.CacheTtlMode.STANDARD, userTurns(3))
        assertEquals(listOf(2 to "5m", 4 to "5m"), messageMarks(std))
        assertFalse(std.getJSONArray("system").getJSONObject(0).getJSONObject("cache_control").has("ttl"))
        val ext = bodyFor(AnthropicProvider.CacheTtlMode.EXTENDED, userTurns(3))
        assertEquals(listOf(2 to "1h", 4 to "1h"), messageMarks(ext))
        assertEquals("1h", ext.getJSONArray("system").getJSONObject(0).getJSONObject("cache_control").getString("ttl"))
    }

    @Test
    fun `smart keeps a 1h checkpoint every 4 user messages and a 5m tail`() {
        // 6 user messages: checkpoint = #4 (index 6), tail = #6 (index 10).
        val b = bodyFor(AnthropicProvider.CacheTtlMode.SMART, userTurns(6))
        assertEquals(listOf(6 to "1h", 10 to "5m"), messageMarks(b))
        assertEquals("1h", b.getJSONArray("system").getJSONObject(0).getJSONObject("cache_control").getString("ttl"))
        // The tail that IS a checkpoint is written with 1h, alone.
        assertEquals(listOf(14 to "1h"), messageMarks(bodyFor(AnthropicProvider.CacheTtlMode.SMART, userTurns(8))))
        // Before the first checkpoint: previous + tail, both 5m.
        assertEquals(listOf(2 to "5m", 4 to "5m"), messageMarks(bodyFor(AnthropicProvider.CacheTtlMode.SMART, userTurns(3))))
        // Long-gap upgrade: tail becomes 1h too; 1h never follows a 5m.
        assertEquals(listOf(6 to "1h", 10 to "1h"), messageMarks(bodyFor(AnthropicProvider.CacheTtlMode.SMART, userTurns(6), longTail = true)))
    }

    @Test
    fun `no request ever puts a 1h breakpoint after a 5m one`() {
        for (mode in AnthropicProvider.CacheTtlMode.values()) for (n in 1..9) for (lt in listOf(false, true)) {
            val ttls = messageMarks(bodyFor(mode, userTurns(n), lt)).map { it.second }
            val firstShort = ttls.indexOf("5m")
            if (firstShort >= 0) {
                assertFalse("$mode n=$n longTail=$lt -> $ttls", ttls.drop(firstShort).contains("1h"))
            }
            assertTrue("at most 2 message breakpoints: $ttls", ttls.size <= 2)
        }
    }

    // ── tool use ─────────────────────────────────────────────────────────

    @Test
    fun `wrap-up turn keeps tools and sends tool_choice none`() = runBlocking {
        val p = provider(LLMModel.claudeOpus48)
        p.forceNoToolUse = true
        val tool = com.openminis.app.data.model.AgentToolDefinition(
            name = "shell_execute",
            description = "run",
            parameters = mapOf("command" to com.openminis.app.data.model.AgentToolParam("string", "cmd")),
        )
        server.enqueue(ok())
        p.sendMessage(listOf(LLMMessage(LLMMessage.Role.USER, "hi")), null, 4096, tools = listOf(tool))
        val body = sent()
        assertEquals(1, body.getJSONArray("tools").length())
        assertEquals("none", body.getJSONObject("tool_choice").getString("type"))
    }

    @Test
    fun `tool_result blocks come first in a merged user turn`() = runBlocking {
        server.enqueue(ok())
        val history = toolLoopHistory(null).dropLast(1) + LLMMessage(
            role = LLMMessage.Role.USER,
            content = "",
            contentParts = listOf(
                AgentContentPart.Text("note first"),
                AgentContentPart.ToolResult("toolu_1", "shell_execute", "a.txt"),
            ),
        )
        provider(LLMModel.claudeHaiku45).sendMessage(history, null, 256)
        val msgs = sent().getJSONArray("messages")
        val last = msgs.getJSONObject(msgs.length() - 1).getJSONArray("content")
        assertEquals("tool_result", last.getJSONObject(0).getString("type"))
        assertEquals("text", last.getJSONObject(1).getString("type"))
    }

    @Test
    fun `tool input given whole in content_block_start is used`() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/event-stream").setBody(
                sse(
                    """{"type":"message_start","message":{"usage":{"input_tokens":5,"output_tokens":1}}}""",
                    """{"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"toolu_9","name":"shell_execute","input":{"command":"pwd"}}}""",
                    """{"type":"content_block_stop","index":0}""",
                    """{"type":"message_delta","delta":{"stop_reason":"tool_use"},"usage":{"output_tokens":9}}""",
                ),
            ),
        )
        val chunks = provider(LLMModel.claudeHaiku45).streamMessage(
            listOf(LLMMessage(LLMMessage.Role.USER, "x")), null, 256,
        ).toList()
        val call = chunks.filterIsInstance<LLMStreamChunk.ToolCallComplete>().single()
        assertEquals("pwd", call.args.getString("command"))
    }
}


package com.openminis.app.provider.gemini

import com.openminis.app.data.model.LLMModel
import com.openminis.app.data.model.ThinkingLevel
import com.openminis.app.data.model.inferContextWindowTokens
import com.openminis.app.provider.thinking.ThinkingRuleResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiNativeCompatTest {

    @Test
    fun `Gemini 4 and future major generations are recognized as thinking and signature-requiring`() {
        for (id in listOf(
            "gemini-4-hydrogen",
            "gemini-4-he",
            "gemini-4.1-pro",
            "gemini-5-pro",
            "gemini-3-pro-preview",
            "gemini-3.1-pro-preview",
        )) {
            assertTrue("$id must support thinking", GeminiProvider.supportsThinking(id))
            assertTrue("$id must require thoughtSignature", GeminiProvider.requiresThoughtSignature(id))
            assertEquals(
                "high",
                ThinkingRuleResolver.geminiThinkingConfig(id, ThinkingLevel.HIGH)?.optString("thinkingLevel"),
            )
        }
    }

    @Test
    fun `Gemini 4 rejects minimal thinkingLevel on OFF and floors at low`() {
        val cfg = ThinkingRuleResolver.geminiThinkingConfig("gemini-4-hydrogen", ThinkingLevel.OFF)
        assertEquals("low", cfg?.optString("thinkingLevel"))
    }

    @Test
    fun `Gemini 4 non-reasoning modalities stay excluded from thinking`() {
        for (id in listOf(
            "gemini-4-flash-tts",
            "gemini-4-pro-image",
            "gemini-4-embedding",
            "gemini-4.1-vision",
            "gemini-2.5-flash-lite",
        )) {
            assertFalse("$id must not claim thinking", GeminiProvider.supportsThinking(id))
            assertNull(ThinkingRuleResolver.geminiThinkingConfig(id, ThinkingLevel.HIGH))
        }
    }

    @Test
    fun `custom base URL discovery builds v1beta and fallback candidate paths`() {
        val bare = GeminiModelsApi.buildCandidateUrls("https://agy.example.com:8443")
        assertEquals("https://agy.example.com:8443/v1beta/models", bare.first())
        assertTrue(bare.contains("https://agy.example.com:8443/models"))

        val v1beta = GeminiModelsApi.buildCandidateUrls("https://agy.example.com:8443/v1beta/")
        assertEquals("https://agy.example.com:8443/v1beta/models", v1beta.first())

        val accidentalV1 = GeminiModelsApi.buildCandidateUrls("https://agy.example.com:8443/v1")
        assertEquals("https://agy.example.com:8443/v1beta/models", accidentalV1.first())
        assertTrue(accidentalV1.contains("https://agy.example.com:8443/v1/models"))
    }

    @Test
    fun `normalizeGeminiBasePath handles bare hosts, accidental v1, and explicit v1beta`() {
        assertEquals(
            "https://agy.example.com:8443/v1beta",
            GeminiProvider.normalizeGeminiBasePath("https://agy.example.com:8443"),
        )
        assertEquals(
            "https://agy.example.com:8443/v1beta",
            GeminiProvider.normalizeGeminiBasePath("https://agy.example.com:8443/v1/"),
        )
        assertEquals(
            "https://agy.example.com:8443/gemini/v1beta",
            GeminiProvider.normalizeGeminiBasePath("https://agy.example.com:8443/gemini/v1beta/"),
        )
    }

    @Test
    fun `parseModelsResponse accepts Gemini native models with or without supportedGenerationMethods and OpenAI data arrays`() {
        val relayNativeJson = """
            {
              "models": [
                {"name": "models/gemini-4-hydrogen", "displayName": "Gemini 4 Hydrogen", "inputTokenLimit": 1048576, "outputTokenLimit": 65536},
                {"name": "models/gemini-4-he", "supportedGenerationMethods": ["generateContent", "streamGenerateContent"]},
                {"name": "models/gemini-embedding-001", "supportedGenerationMethods": ["embedContent"]}
              ]
            }
        """.trimIndent()
        val parsed = GeminiModelsApi.parseModelsResponse(relayNativeJson, isCustomEndpoint = true)
        assertEquals(listOf("gemini-4-hydrogen", "gemini-4-he"), parsed.map { it.id })
        assertEquals(true, parsed[0].supportsReasoning)
        assertEquals(1_048_576, parsed[0].contextWindow)
        assertEquals(true, parsed[1].supportsReasoning)

        val openAiShape = """
            {
              "object": "list",
              "data": [
                {"id": "gemini-4-hydrogen"},
                {"id": "gemini-3.1-pro-preview"}
              ]
            }
        """.trimIndent()
        val parsedOpenAi = GeminiModelsApi.parseModelsResponse(openAiShape, isCustomEndpoint = true)
        assertEquals(listOf("gemini-4-hydrogen", "gemini-3.1-pro-preview"), parsedOpenAi.map { it.id })
        assertEquals(true, parsedOpenAi[0].supportsReasoning)
    }

    @Test
    fun `Gemini 4 context window resolves to 1M in both loop and slider heuristics`() {
        val m = LLMModel("gemini-4-hydrogen", "Gemini 4 Hydrogen", "Google")
        assertEquals(1_000_000, m.contextWindowTokens)
        assertEquals(1_000_000, inferContextWindowTokens(m))
    }
}

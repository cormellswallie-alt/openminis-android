package com.openminis.app.provider.anthropic

import com.openminis.app.ProductionSources
import com.openminis.app.auth.ClaudeOAuthManager
import com.openminis.app.data.model.LLMModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests ensuring builds without `ANTHROPIC_OAUTH_IDENTIFIER_PROMPT`
 * never throw Claude Code OAuth configuration errors when using OpenAI, Gemini,
 * or non-OAuth (API-key / third-party relay) Anthropic providers.
 */
class ClaudeOAuthPromptGuardTest {

    @Test
    fun `AnthropicProvider API-key resolveSystemPrompt does not touch throwing OAuth prompt getter`() {
        val provider = AnthropicProvider(
            apiKey = "sk-ant-test",
            model = LLMModel.claudeSonnet46,
            basePath = "https://api.anthropic.com",
            isOAuth = false,
        )
        assertNull(provider.resolveSystemPrompt(null))
        assertNull(provider.resolveSystemPrompt(""))

        val blocks = provider.resolveSystemPrompt("You are a helpful assistant.")
        requireNotNull(blocks)
        assertEquals(1, blocks.length())
        assertEquals("You are a helpful assistant.", blocks.getJSONObject(0).getString("text"))
    }

    @Test
    fun `ChatViewModel systemPromptFor gates ANTHROPIC_OAUTH_IDENTIFIER_PROMPT behind oauth check`() {
        val vm = ProductionSources.read("ui/chat/ChatViewModel.kt")
        val fnBody = vm.substringAfter("private fun systemPromptFor(")
            .substringBefore("private fun isBoundToAbandonedModel()")
        val oauthGateIdx = fnBody.indexOf("if (oauth)")
        val throwingReadIdx = fnBody.indexOf("ANTHROPIC_OAUTH_IDENTIFIER_PROMPT")
        assertTrue(
            "systemPromptFor must check `if (oauth)` before reading ANTHROPIC_OAUTH_IDENTIFIER_PROMPT",
            oauthGateIdx in 0 until throwingReadIdx,
        )
        assertTrue(
            "Non-OAuth path must use non-throwing configuredOAuthIdentifierPrompt",
            fnBody.contains("configuredOAuthIdentifierPrompt"),
        )
    }

    @Test
    fun `AnthropicProvider resolveSystemPrompt gates ANTHROPIC_OAUTH_IDENTIFIER_PROMPT behind isOAuth`() {
        val src = ProductionSources.read("provider/anthropic/AnthropicProvider.kt")
        val fnBody = src.substringAfter("internal fun resolveSystemPrompt(")
            .substringBefore("private fun buildRequestBody(")
        val oauthGateIdx = fnBody.indexOf("if (isOAuth)")
        val throwingReadIdx = fnBody.indexOf("ANTHROPIC_OAUTH_IDENTIFIER_PROMPT")
        assertTrue(
            "resolveSystemPrompt must check `if (isOAuth)` before reading ANTHROPIC_OAUTH_IDENTIFIER_PROMPT",
            oauthGateIdx in 0 until throwingReadIdx,
        )
    }

    @Test
    fun `configuredOAuthIdentifierPrompt is safe to access when unconfigured`() {
        // Must never throw even in public mirror builds where BuildConfig field is empty.
        val value = ClaudeOAuthManager.configuredOAuthIdentifierPrompt
        assertTrue(value == null || value.isNotEmpty())
    }
}

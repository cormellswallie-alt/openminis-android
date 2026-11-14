package com.openminis.app.tools

import com.openminis.app.ProductionSources
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AskUserTool] ("Model Questionnaire") schema serialization,
 * tolerant input parsing, result round-tripping, and system prompt integration.
 */
class AskUserToolTest {

    @Test
    fun `definition serializes nested questions array for OpenAI Anthropic and Gemini`() {
        val openAiJson = JSONObject(AskUserTool.definition.toJson())
        assertEquals("ask_user", openAiJson.getString("name"))
        val props = openAiJson.getJSONObject("input_schema").getJSONObject("properties")
        val questionsProp = props.getJSONObject("questions")
        assertEquals("array", questionsProp.getString("type"))
        val itemProps = questionsProp.getJSONObject("items").getJSONObject("properties")
        assertTrue(itemProps.has("question"))
        assertTrue(itemProps.has("type"))
        assertTrue(itemProps.has("options"))

        val geminiJson = JSONObject(AskUserTool.definition.toGeminiJson())
        assertEquals("ask_user", geminiJson.getString("name"))
        val geminiQuestions = geminiJson
            .getJSONObject("parameters")
            .getJSONObject("properties")
            .getJSONObject("questions")
        assertEquals("ARRAY", geminiQuestions.getString("type"))
        assertEquals(
            "OBJECT",
            geminiQuestions.getJSONObject("items").getString("type"),
        )
    }

    @Test
    fun `parseSpec handles structured single multi and text questions`() {
        val raw = """
            {
              "title": "Project Setup",
              "questions": [
                {
                  "id": "framework",
                  "header": "Framework",
                  "question": "Which framework should we use?",
                  "type": "single_select",
                  "options": ["Compose", "Views"]
                },
                {
                  "id": "features",
                  "question": "Select extra modules",
                  "multi_select": true,
                  "options": ["Auth", "Offline", "Analytics"]
                },
                {
                  "question": "Any custom package name?",
                  "type": "text",
                  "placeholder": "com.example.app"
                }
              ]
            }
        """.trimIndent()

        val spec = AskUserTool.parseSpec(raw)
        assertNotNull(spec)
        assertEquals("Project Setup", spec!!.title)
        assertEquals(3, spec.questions.size)
        assertEquals(AskUserTool.QuestionKind.SINGLE_SELECT, spec.questions[0].kind)
        assertEquals(listOf("Compose", "Views"), spec.questions[0].options)
        assertEquals(AskUserTool.QuestionKind.MULTI_SELECT, spec.questions[1].kind)
        assertEquals(AskUserTool.QuestionKind.TEXT, spec.questions[2].kind)
        assertEquals("com.example.app", spec.questions[2].placeholder)
    }

    @Test
    fun `result serializes and parses back cleanly`() {
        val result = AskUserTool.QuestionnaireResult(
            status = "answered",
            answers = listOf(
                AskUserTool.AnswerEntry(
                    id = "framework",
                    question = "Which framework should we use?",
                    selected = listOf("Compose"),
                    text = "Compose",
                ),
            ),
        )
        val json = result.toJsonString()
        val parsed = AskUserTool.parseResult(json)
        assertNotNull(parsed)
        assertEquals("answered", parsed!!.status)
        assertEquals(1, parsed.answers.size)
        assertEquals("Compose", parsed.answers[0].text)
    }

    @Test
    fun `ChatViewModel system prompt documents ask_user questionnaire tool`() {
        val vmSource = ProductionSources.read("ui/chat/ChatViewModel.kt")
        assertTrue(
            "buildSystemPrompt must list ask_user in built-in tools",
            vmSource.contains("- ask_user: Present an interactive questionnaire"),
        )
        assertTrue(
            "buildSystemPrompt must include Model questionnaire guidance section",
            vmSource.contains("Model questionnaire (ask_user):"),
        )
        assertFalse(
            "helper sessions must not expose ask_user",
            makeAgentTools(askUserEnabled = true, isHelper = true).any { it.name == AskUserTool.NAME },
        )
        assertTrue(
            "main sessions expose ask_user when enabled",
            makeAgentTools(askUserEnabled = true, isHelper = false).any { it.name == AskUserTool.NAME },
        )
    }
}

package com.openminis.app.tools

import com.openminis.app.data.model.AgentToolDefinition
import com.openminis.app.data.model.AgentToolParam
import org.json.JSONArray
import org.json.JSONObject

/**
 * Built-in interactive Model Questionnaire tool (`ask_user`).
 *
 * Allows the model to ask 1–6 structured single-select, multi-select, or
 * free-text questions mid-turn and suspend until the user submits answers in
 * the native M3 Expressive questionnaire card/sheet, keeping the agent loop
 * running without breaking the conversation iteration.
 */
object AskUserTool {

    const val NAME = "ask_user"

    enum class QuestionKind(val wire: String) {
        SINGLE_SELECT("single_select"),
        MULTI_SELECT("multi_select"),
        TEXT("text");

        companion object {
            fun fromWire(raw: String?, hasOptions: Boolean, multiSelectFlag: Boolean): QuestionKind {
                if (multiSelectFlag) return MULTI_SELECT
                return when (raw?.trim()?.lowercase()) {
                    "multi_select", "multiselect", "multiple", "checkbox" -> MULTI_SELECT
                    "text", "free_text", "input", "open" -> TEXT
                    "single_select", "singleselect", "single", "radio" ->
                        if (hasOptions) SINGLE_SELECT else TEXT
                    else -> if (hasOptions) SINGLE_SELECT else TEXT
                }
            }
        }
    }

    data class QuestionOption(
        val label: String,
        val description: String? = null,
    )

    data class QuestionItem(
        val id: String,
        val header: String? = null,
        val question: String,
        val kind: QuestionKind,
        val options: List<QuestionOption> = emptyList(),
        val allowOther: Boolean = true,
        val placeholder: String? = null,
    )

    data class QuestionnaireSpec(
        val toolTitle: String,
        val questions: List<QuestionItem>,
    )

    data class QuestionAnswer(
        val id: String,
        val question: String,
        val header: String? = null,
        val selected: List<String> = emptyList(),
        val customText: String? = null,
    ) {
        val displaySummary: String
            get() {
                val parts = buildList {
                    addAll(selected.filter { it.isNotBlank() })
                    customText?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                }
                return parts.joinToString(", ")
            }
    }

    data class QuestionnaireResult(
        val status: String, // "answered" | "skipped" | "cancelled"
        val answers: List<QuestionAnswer> = emptyList(),
        val note: String? = null,
    ) {
        fun toJsonString(): String {
            val root = JSONObject()
            root.put("status", status)
            if (answers.isNotEmpty()) {
                val arr = JSONArray()
                for (a in answers) {
                    val obj = JSONObject()
                    obj.put("id", a.id)
                    if (!a.header.isNullOrBlank()) obj.put("header", a.header)
                    obj.put("question", a.question)
                    val combined = buildList {
                        addAll(a.selected.filter { it.isNotBlank() })
                        a.customText?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                    }
                    obj.put("selected", JSONArray(a.selected))
                    if (!a.customText.isNullOrBlank()) {
                        obj.put("custom_text", a.customText.trim())
                    }
                    obj.put(
                        "answer",
                        when {
                            combined.isEmpty() -> ""
                            combined.size == 1 -> combined.first()
                            else -> combined.joinToString("; ")
                        },
                    )
                    arr.put(obj)
                }
                root.put("answers", arr)
            }
            if (!note.isNullOrBlank()) {
                root.put("note", note)
            }
            return root.toString()
        }
    }

    val definition: AgentToolDefinition = AgentToolDefinition(
        name = NAME,
        description = "Present an interactive questionnaire UI to ask the user 1–5 concise clarifying questions " +
            "(single-choice, multi-select, or short free-text) WITHOUT ending your turn. " +
            "Execution pauses until the user submits their answers, then returns structured JSON answers " +
            "so you can immediately continue working in the same turn. " +
            "Prefer single_select or multi_select with clear options (put recommended choice first, e.g. 'Option A (Recommended)').",
        parameters = mapOf(
            "tool_title" to AgentToolParam(
                type = "string",
                description = "Brief 4-10 word summary of what you are asking, in the user's language (e.g., '确认架构方案与界面偏好').",
            ),
            "questions" to AgentToolParam(
                type = "array",
                description = "List of 1 to 5 questions to ask the user in a single interactive card.",
                items = AgentToolParam(
                    type = "object",
                    description = "One question item in the questionnaire.",
                    properties = mapOf(
                        "id" to AgentToolParam(
                            type = "string",
                            description = "Short unique key for this question (e.g., 'framework', 'style').",
                        ),
                        "header" to AgentToolParam(
                            type = "string",
                            description = "Short 1-3 word category badge shown above the question (e.g., '技术栈', '部署方式').",
                        ),
                        "question" to AgentToolParam(
                            type = "string",
                            description = "The clear, concise question text shown to the user.",
                        ),
                        "type" to AgentToolParam(
                            type = "string",
                            description = "Question input mode: 'single_select' (choose one option), 'multi_select' (choose one or more options), or 'text' (free-form text input).",
                            enumValues = listOf("single_select", "multi_select", "text"),
                        ),
                        "options" to AgentToolParam(
                            type = "array",
                            description = "Selectable option labels for single_select or multi_select (2-6 concise choices; put recommended option first). Omit or leave empty for 'text' questions.",
                            items = AgentToolParam(
                                type = "string",
                                description = "Concise option text (optionally 'Label — brief description').",
                            ),
                        ),
                        "allow_other" to AgentToolParam(
                            type = "boolean",
                            description = "Whether to include a custom 'Other / Custom input' field alongside the options (default: true).",
                        ),
                        "placeholder" to AgentToolParam(
                            type = "string",
                            description = "Optional placeholder hint for text input or custom 'Other' input.",
                        ),
                    ),
                    required = listOf("question"),
                ),
            ),
        ),
        required = listOf("tool_title", "questions"),
        propertyOrdering = listOf("tool_title", "questions"),
    )

    /**
     * Tolerant parser accepting:
     * 1. Standard `{ "tool_title": "...", "questions": [ { ... }, ... ] }`
     * 2. Stringified JSON array in `"questions": "[...]"`
     * 3. Single-question shorthand `{ "question": "...", "options": [...] }`
     */
    fun parseSpec(argsJson: String): QuestionnaireSpec? {
        if (argsJson.isBlank()) return null
        val root = try {
            JSONObject(argsJson)
        } catch (_: Exception) {
            return null
        }
        val title = root.optString("tool_title", "").trim().ifEmpty { "Questionnaire" }
        val rawQuestions: JSONArray? = when (val qVal = root.opt("questions")) {
            is JSONArray -> qVal
            is String -> runCatching { JSONArray(qVal) }.getOrNull()
            else -> null
        }

        val parsed = mutableListOf<QuestionItem>()
        if (rawQuestions != null) {
            for (i in 0 until rawQuestions.length().coerceAtMost(8)) {
                val elem = rawQuestions.opt(i)
                when (elem) {
                    is JSONObject -> parseQuestionObject(elem, index = i)?.let { parsed.add(it) }
                    is String -> {
                        val trimmed = elem.trim()
                        if (trimmed.isNotEmpty()) {
                            parsed.add(
                                QuestionItem(
                                    id = "q${i + 1}",
                                    question = trimmed,
                                    kind = QuestionKind.TEXT,
                                    allowOther = true,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Fallback: top-level single question shorthand
        if (parsed.isEmpty()) {
            parseQuestionObject(root, index = 0)?.let { parsed.add(it) }
        }

        if (parsed.isEmpty()) return null
        return QuestionnaireSpec(toolTitle = title, questions = parsed)
    }

    private fun parseQuestionObject(obj: JSONObject, index: Int): QuestionItem? {
        val qText = obj.optString("question", "")
            .ifBlank { obj.optString("prompt", "") }
            .ifBlank { obj.optString("title", "") }
            .trim()
        if (qText.isEmpty()) return null

        val id = obj.optString("id", "").trim().ifEmpty { "q${index + 1}" }
        val header = obj.optString("header", "")
            .ifBlank { obj.optString("tag", "") }
            .trim()
            .takeIf { it.isNotEmpty() }

        val options = mutableListOf<QuestionOption>()
        val rawOpts = when (val o = obj.opt("options")) {
            is JSONArray -> o
            is String -> runCatching { JSONArray(o) }.getOrNull()
            else -> null
        }
        if (rawOpts != null) {
            for (j in 0 until rawOpts.length().coerceAtMost(12)) {
                when (val item = rawOpts.opt(j)) {
                    is String -> {
                        val t = item.trim()
                        if (t.isNotEmpty()) {
                            val sepIdx = t.indexOf(" — ").takeIf { it > 0 }
                                ?: t.indexOf(" - ").takeIf { it in 1..36 }
                            if (sepIdx != null) {
                                options.add(
                                    QuestionOption(
                                        label = t.substring(0, sepIdx).trim(),
                                        description = t.substring(sepIdx + 3).trim().takeIf { it.isNotEmpty() },
                                    ),
                                )
                            } else {
                                options.add(QuestionOption(label = t))
                            }
                        }
                    }
                    is JSONObject -> {
                        val lbl = item.optString("label", "")
                            .ifBlank { item.optString("title", "") }
                            .ifBlank { item.optString("value", "") }
                            .trim()
                        if (lbl.isNotEmpty()) {
                            val desc = item.optString("description", "")
                                .ifBlank { item.optString("subtitle", "") }
                                .trim()
                                .takeIf { it.isNotEmpty() }
                            options.add(QuestionOption(label = lbl, description = desc))
                        }
                    }
                }
            }
        }

        val multiFlag = obj.optBoolean("is_multi_select", false) ||
            obj.optBoolean("multi_select", false) ||
            obj.optBoolean("multiple", false)
        val kind = QuestionKind.fromWire(
            raw = obj.optString("type", ""),
            hasOptions = options.isNotEmpty(),
            multiSelectFlag = multiFlag,
        )
        val allowOther = if (obj.has("allow_other")) obj.optBoolean("allow_other", true) else true
        val placeholder = obj.optString("placeholder", "").trim().takeIf { it.isNotEmpty() }

        return QuestionItem(
            id = id,
            header = header,
            question = qText,
            kind = kind,
            options = options,
            allowOther = allowOther,
            placeholder = placeholder,
        )
    }

    fun parseResult(contentJson: String): QuestionnaireResult? {
        if (contentJson.isBlank()) return null
        val root = try {
            JSONObject(contentJson)
        } catch (_: Exception) {
            return null
        }
        val status = root.optString("status", "").trim().ifEmpty { return null }
        val answersArr = root.optJSONArray("answers")
        val answers = mutableListOf<QuestionAnswer>()
        if (answersArr != null) {
            for (i in 0 until answersArr.length()) {
                val aObj = answersArr.optJSONObject(i) ?: continue
                val selArr = aObj.optJSONArray("selected")
                val selected = buildList {
                    if (selArr != null) {
                        for (j in 0 until selArr.length()) {
                            val s = selArr.optString(j, "").trim()
                            if (s.isNotEmpty()) add(s)
                        }
                    } else {
                        val fallbackAns = aObj.optString("answer", "").trim()
                        if (fallbackAns.isNotEmpty()) add(fallbackAns)
                    }
                }
                answers.add(
                    QuestionAnswer(
                        id = aObj.optString("id", "q${i + 1}"),
                        question = aObj.optString("question", ""),
                        header = aObj.optString("header", "").takeIf { it.isNotEmpty() },
                        selected = selected,
                        customText = aObj.optString("custom_text", "").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
        val note = root.optString("note", "").takeIf { it.isNotBlank() }
        return QuestionnaireResult(
            status = status,
            answers = answers,
            note = note,
        )
    }
}

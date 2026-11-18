package com.openminis.app.provider.gemini

import android.content.Context
import com.openminis.app.data.model.LLMModel
import com.openminis.app.provider.ModelsDevApi
import com.openminis.app.provider.ProviderModelsCache
import com.openminis.app.provider.applyUserAgentOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

object GeminiModelsApi {
    private const val OFFICIAL_BASE = "https://generativelanguage.googleapis.com/v1beta"
    private val client = OkHttpClient()
    private val cache = ProviderModelsCache("gemini")

    /**
     * Fetch the Gemini model catalog from the official API or a Gemini-native
     * compatible endpoint ([baseURL]).
     *
     * Supports:
     *   - Official API (`generativelanguage.googleapis.com`) via `?key=` or OAuth Bearer
     *   - Gemini-native compatible gateways (`baseURL` supplied): probes the
     *     configured base first (`.../models` when `baseURL` already carries a
     *     version path such as `/v1beta`, otherwise `.../v1beta/models` then
     *     `.../models` / `.../v1/models`), climbs parent path segments on 404,
     *     and parses both Gemini-native `{"models":[...]}` and OpenAI-style
     *     `{"data":[...]}` catalogs without filtering out models that omit
     *     `supportedGenerationMethods`.
     *   - Never contaminates a third-party custom endpoint with the hardcoded
     *     official `LLMModel.allGemini` list on failure — returns empty so the
     *     caller's catalog fallback / existing entries handle it cleanly.
     */
    suspend fun fetchModels(
        apiKey: String,
        baseURL: String? = null,
        isOAuth: Boolean = false,
        cloudCodeFallback: Boolean = false,
        context: Context? = null,
        forceRefresh: Boolean = false,
        customUserAgent: String? = null,
    ): List<LLMModel> = withContext(Dispatchers.IO) {
        val normalizedBase = baseURL?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
        val isCustomEndpoint = normalizedBase != null &&
            !normalizedBase.lowercase().contains("generativelanguage.googleapis.com")

        if (cloudCodeFallback && !isCustomEndpoint) return@withContext LLMModel.allGemini

        val cacheKey = buildString {
            append(if (isOAuth) "oauth|" else "key|")
            append(normalizedBase ?: "default")
            append('|')
            append(apiKey)
        }
        if (context != null && !forceRefresh) {
            cache.load(context, cacheKey)?.let { return@withContext it }
        }

        val candidateUrls = buildCandidateUrls(normalizedBase)
        for ((idx, rawUrl) in candidateUrls.withIndex()) {
            val builder = Request.Builder()
            if (isOAuth) {
                builder.url(rawUrl)
                if (apiKey.isNotBlank()) {
                    builder.header("Authorization", "Bearer $apiKey")
                }
            } else if (isCustomEndpoint) {
                val urlWithKey = if (apiKey.isNotBlank()) appendKeyParam(rawUrl, apiKey) else rawUrl
                builder.url(urlWithKey)
                if (apiKey.isNotBlank()) {
                    builder.header("x-goog-api-key", apiKey)
                    builder.header("Authorization", "Bearer $apiKey")
                }
            } else {
                builder.url(appendKeyParam(rawUrl, apiKey))
            }

            // [T-android-default-ua] brand outbound /v1beta/models request (or apply custom UA).
            builder.applyUserAgentOverride(if (isCustomEndpoint) customUserAgent else null)

            val response: Response = try {
                client.newCall(builder.build()).execute()
            } catch (e: Exception) {
                android.util.Log.w("GeminiModels", "Fetch error (candidate=$idx url=$rawUrl): ${e.message}")
                continue
            }
            val body = response.use { it.body?.string().orEmpty() }

            if (!response.isSuccessful) {
                // 403 on official OAuth almost always means the token lacks the
                // generative-language scope — fall back to built-in list.
                if (!isCustomEndpoint && isOAuth && response.code == 403) {
                    return@withContext LLMModel.allGemini
                }
                if (context != null && (response.code == 401 || response.code == 403)) {
                    cache.invalidate(context, cacheKey)
                }
                android.util.Log.w(
                    "GeminiModels",
                    "Fetch HTTP ${response.code} (candidate=$idx url=$rawUrl): ${body.take(180)}",
                )
                continue
            }

            val parsed = parseModelsResponse(body, isCustomEndpoint = isCustomEndpoint)
            if (parsed.isNotEmpty()) {
                val enriched = ModelsDevApi.enrichModels(parsed)
                if (context != null) cache.save(context, cacheKey, enriched)
                return@withContext enriched
            }
        }

        return@withContext if (isCustomEndpoint) emptyList() else LLMModel.allGemini
    }

    internal fun buildCandidateUrls(baseURL: String?): List<String> {
        if (baseURL.isNullOrBlank()) return listOf("$OFFICIAL_BASE/models")
        val seen = LinkedHashSet<String>()
        var current: String? = baseURL.trim().trimEnd('/')
        var depth = 0
        while (current != null && depth < 4) {
            val lower = current.lowercase()
            when {
                lower.endsWith("/models") -> {
                    seen.add(current)
                }
                lower.endsWith("/v1beta") || lower.endsWith("/v1alpha") -> {
                    seen.add("$current/models")
                }
                lower.endsWith("/v1") -> {
                    // When appendV1Suffix turned a bare host into .../v1 on a
                    // Gemini-native relay, prefer /v1beta/models at the parent
                    // root right after .../v1/models so native v1beta catalogs
                    // (including 4.x models) win over missing /v1 routes.
                    val root = current.dropLast(3)
                    seen.add("$root/v1beta/models")
                    seen.add("$current/models")
                }
                else -> {
                    seen.add("$current/v1beta/models")
                    seen.add("$current/models")
                    seen.add("$current/v1/models")
                }
            }
            current = parentPath(current)
            depth++
        }
        return seen.toList()
    }

    internal fun parseModelsResponse(body: String, isCustomEndpoint: Boolean = false): List<LLMModel> {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            val result = mutableListOf<LLMModel>()
            val seenIds = HashSet<String>()

            fun addCandidate(
                rawId: String,
                rawDisplay: String?,
                contextWindow: Int? = null,
                maxOutput: Int? = null,
                explicitThinking: Boolean? = null,
            ) {
                val cleanId = rawId.trim().removePrefix("models/").trim()
                if (cleanId.isEmpty() || !seenIds.add(cleanId)) return
                if (isNonChatUtilityModel(cleanId)) return
                val display = rawDisplay?.trim().orEmpty().ifBlank { LLMModel.modelDisplayName(cleanId) }
                val supportsReasoning = explicitThinking
                    ?: if (GeminiProvider.supportsThinking(cleanId)) true else null
                result.add(
                    LLMModel(
                        id = cleanId,
                        displayName = display,
                        provider = "Google",
                        contextWindow = contextWindow?.takeIf { it > 0 },
                        maxOutputTokens = maxOutput?.takeIf { it > 0 },
                        supportsReasoning = supportsReasoning,
                    ),
                )
            }

            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                parseModelArray(arr, isCustomEndpoint, ::addCandidate)
                return result
            }

            val json = JSONObject(trimmed)
            val modelsField = json.opt("models")
            when (modelsField) {
                is JSONArray -> parseModelArray(modelsField, isCustomEndpoint, ::addCandidate)
                is JSONObject -> {
                    val keys = modelsField.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val meta = modelsField.optJSONObject(key)
                        val display = meta?.optString("displayName", "")
                            ?.ifBlank { meta.optString("display_name", "") }
                        val ctx = meta?.optInt("inputTokenLimit", 0)?.takeIf { it > 0 }
                            ?: meta?.optInt("context_window", 0)?.takeIf { it > 0 }
                        val maxOut = meta?.optInt("outputTokenLimit", 0)?.takeIf { it > 0 }
                            ?: meta?.optInt("max_output_tokens", 0)?.takeIf { it > 0 }
                        val thinking = if (meta != null && meta.has("thinking")) meta.optBoolean("thinking") else null
                        addCandidate(key, display, ctx, maxOut, thinking)
                    }
                }
            }

            if (result.isEmpty()) {
                val dataArr = json.optJSONArray("data")
                if (dataArr != null) {
                    parseModelArray(dataArr, isCustomEndpoint = true, ::addCandidate)
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private inline fun parseModelArray(
        arr: JSONArray,
        isCustomEndpoint: Boolean,
        addCandidate: (String, String?, Int?, Int?, Boolean?) -> Unit,
    ) {
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            if (item is String) {
                addCandidate(item, null, null, null, null)
                continue
            }
            val obj = item as? JSONObject ?: continue
            val rawName = obj.optString("name", "").ifBlank { obj.optString("id", "") }
            if (rawName.isBlank()) continue

            val methods = obj.optJSONArray("supportedGenerationMethods")
            if (methods != null && methods.length() > 0) {
                val supportsGen = (0 until methods.length()).any { idx ->
                    val m = methods.optString(idx, "")
                    m.contains("generateContent", ignoreCase = true) ||
                        m.equals("chat", ignoreCase = true)
                }
                if (!supportsGen) continue
            } else if (!isCustomEndpoint) {
                // Official Google /v1beta/models always populates supportedGenerationMethods;
                // entries without it on the official host are non-generative artifacts.
                // Third-party Gemini-compatible relays frequently omit the field.
                if (obj.has("supportedGenerationMethods")) continue
            }

            val displayName = obj.optString("displayName", "")
                .ifBlank { obj.optString("display_name", "") }
            val inputLimit = obj.optInt("inputTokenLimit", 0).takeIf { it > 0 }
                ?: obj.optInt("context_window", 0).takeIf { it > 0 }
                ?: obj.optInt("context_length", 0).takeIf { it > 0 }
            val outputLimit = obj.optInt("outputTokenLimit", 0).takeIf { it > 0 }
                ?: obj.optInt("max_output_tokens", 0).takeIf { it > 0 }
            val explicitThinking: Boolean? = when {
                obj.has("thinking") -> obj.optBoolean("thinking")
                obj.has("supports_reasoning") -> obj.optBoolean("supports_reasoning")
                else -> null
            }
            addCandidate(rawName, displayName, inputLimit, outputLimit, explicitThinking)
        }
    }

    private fun isNonChatUtilityModel(id: String): Boolean {
        val lower = id.lowercase()
        return lower.startsWith("embedding-") ||
            lower.startsWith("text-embedding") ||
            lower.contains("-embedding-") ||
            lower.endsWith("-embedding") ||
            lower == "aqa"
    }

    private fun appendKeyParam(url: String, apiKey: String): String {
        val httpUrl = url.toHttpUrlOrNull()
            ?: return if ('?' in url) "$url&key=$apiKey" else "$url?key=$apiKey"
        return httpUrl.newBuilder()
            .setQueryParameter("key", apiKey)
            .build()
            .toString()
    }

    private fun parentPath(base: String): String? {
        val httpUrl = base.toHttpUrlOrNull() ?: return null
        val segments = httpUrl.pathSegments.toMutableList()
        while (segments.isNotEmpty() && segments.last().isEmpty()) {
            segments.removeAt(segments.size - 1)
        }
        if (segments.isEmpty()) return null
        segments.removeAt(segments.size - 1)
        val builder = httpUrl.newBuilder()
        for (i in httpUrl.pathSegments.indices.reversed()) {
            builder.removePathSegment(i)
        }
        for (seg in segments) {
            builder.addPathSegment(seg)
        }
        return builder.build().toString().trimEnd('/')
    }
}

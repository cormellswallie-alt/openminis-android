package com.openminis.app.provider.anthropic

import android.util.Base64
import com.openminis.app.data.model.AgentContentPart
import com.openminis.app.data.model.AgentToolDefinition
import com.openminis.app.data.model.sanitizeToolId
import com.openminis.app.data.model.LLMError
import com.openminis.app.data.model.LLMMessage
import com.openminis.app.data.model.LLMModel
import com.openminis.app.data.model.LLMResponse
import com.openminis.app.data.model.LLMStreamChunk
import com.openminis.app.data.model.LLMUsage
import com.openminis.app.data.model.ThinkingLevel
import com.openminis.app.provider.ImageBudget
import com.openminis.app.provider.LLMProvider
import com.openminis.app.provider.applyUserAgentOverride
import com.openminis.app.provider.safeOptString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import com.openminis.app.provider.failOnSilentEmptyCompletion
import com.openminis.app.provider.thinking.ThinkingRuleResolver

class AnthropicProvider(
    private val apiKey: String,
    override var model: LLMModel = LLMModel.claudeHaiku45,
    private val basePath: String = "https://api.anthropic.com",
    /** Whether this provider uses OAuth credentials (Bearer + beta header). */
    val isOAuth: Boolean = false,
    /**
     * [T-provider-custom-user-agent] Per-provider User-Agent override.
     * null/blank → default UA; non-blank → replaces User-Agent on the chat
     * request. Only set for custom-base Anthropic-compat instances.
     */
    private val customUserAgent: String? = null,
) : LLMProvider {
    override val name = "Anthropic"
    override val defaultMaxOutputTokens: Int get() = 64_000

    /**
     * [T-android-enhanced-cache] Enhanced Cache toggle — when true, every
     * `cache_control: ephemeral` breakpoint carries `ttl:"1h"` (1-hour cache
     * TTL) instead of the default 5-minute TTL, and the
     * `extended-cache-ttl-2025-04-11` beta flag is attached to API-key requests
     * (OAuth already carries it). Set per chat turn from
     * `ChatViewModel.enhancedCacheEnabled`; defaults off so existing behaviour
     * is unchanged. Mirrors iOS `RequestBodyPatcher.setExtendedCacheTTL`.
     */
    var enhancedCache: Boolean = false

    /**
     * [T-anthropic-wire-compat] Optional, documented request features that a
     * relay / older backend might not understand. If a request carrying any of
     * them is answered with HTTP 400, it is retried once without them; when
     * that retry succeeds they stay off for this provider instance, so one
     * incompatible relay costs one extra request instead of every request.
     */
    enum class WireFeature {
        /** Replaying signed `thinking` / `redacted_thinking` blocks. */
        SIGNED_THINKING_ECHO,
        /** `thinking.type` = "disabled" / "between_tools" on Claude 5.x. */
        EXPLICIT_THINKING_OFF,
        /** `output_config.effort` = "xhigh" (Claude 4.7+). */
        XHIGH_EFFORT,
    }

    @Volatile
    internal var disabledFeatures: Set<WireFeature> = emptySet()
        private set

    /**
     * Build a `cache_control` object honoring [enhancedCache]. Default 5-minute
     * TTL is `{"type":"ephemeral"}`; enhanced is `{"type":"ephemeral","ttl":"1h"}`.
     * Every ephemeral breakpoint in the request body routes through here.
     */
    private fun ephemeralCacheControl(): JSONObject =
        JSONObject().put("type", "ephemeral").apply {
            if (enhancedCache) put("ttl", "1h")
        }

    /** Use Bearer auth for custom (non-Anthropic) base URLs or OAuth. */
    private val isCustomEndpoint: Boolean = basePath != "https://api.anthropic.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        // [T-android-stale-conn-retry-hang] Shared pool — see NetworkMonitor.
        // Network-transition eviction must reach provider connections.
        .connectionPool(com.openminis.app.network.NetworkMonitor.sharedLLMConnectionPool)
        .build()

    override suspend fun sendMessageClamped(
        messages: List<LLMMessage>,
        systemPrompt: String?,
        maxTokens: Int,
        temperature: Double?,
        imageParts: List<LLMMessage.ImagePart>,
        tools: List<AgentToolDefinition>,
        thinkingLevel: ThinkingLevel,
    ): LLMResponse = withContext(Dispatchers.IO) {
        val (response, _, _) = executeWithCompatRetry { suppress, used ->
            buildRequestBody(messages, systemPrompt, maxTokens, stream = false, temperature = temperature, imageParts = imageParts, tools = tools, thinkingLevel = thinkingLevel, suppress = suppress, used = used)
        }
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw mapHttpError(response.code, responseBody)
        }

        val json = JSONObject(responseBody)
        parseResponse(json)
    }

    override fun streamMessageClamped(
        messages: List<LLMMessage>,
        systemPrompt: String?,
        maxTokens: Int,
        temperature: Double?,
        imageParts: List<LLMMessage.ImagePart>,
        tools: List<AgentToolDefinition>,
        thinkingLevel: ThinkingLevel,
    ): Flow<LLMStreamChunk> = rawStreamMessage(
        messages, systemPrompt, maxTokens, temperature, imageParts, tools, thinkingLevel,
    ).failOnSilentEmptyCompletion(name)

    private fun rawStreamMessage(
        messages: List<LLMMessage>,
        systemPrompt: String?,
        maxTokens: Int,
        temperature: Double?,
        imageParts: List<LLMMessage.ImagePart>,
        tools: List<AgentToolDefinition>,
        thinkingLevel: ThinkingLevel,
    ): Flow<LLMStreamChunk> = callbackFlow {
        val startTime = System.currentTimeMillis()
        // T302: the body is serialized exactly once per attempt inside
        // executeWithCompatRetry and the string reused for the request.
        val (response, bodyStr, request) = executeWithCompatRetry { suppress, used ->
            buildRequestBody(messages, systemPrompt, maxTokens, stream = true, temperature = temperature, imageParts = imageParts, tools = tools, thinkingLevel = thinkingLevel, suppress = suppress, used = used)
        }
        val durationMs = System.currentTimeMillis() - startTime

        // Capture request headers as map
        val headerMap = mutableMapOf<String, String>()
        for (name in request.headers.names()) {
            headerMap[name] = request.headers[name] ?: ""
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            response.close()
            // T302: skip the LLMRequestLog write entirely on release builds —
            // see OpenAIProvider for the full rationale (release users never
            // reach debug.llmRequests, so retaining multi-MB request bodies
            // there is pure OOM risk for zero benefit).
            if (com.openminis.app.BuildConfig.DEBUG) {
                com.openminis.app.debug.LLMRequestLog.add(
                    com.openminis.app.debug.LLMRequestLog.Entry(
                        provider = "anthropic",
                        requestURL = request.url.toString(),
                        requestHeaders = headerMap,
                        requestBody = bodyStr,
                        durationMs = durationMs,
                        responseStatusCode = response.code,
                        responseBody = errorBody.take(2000),
                    )
                )
            }
            android.util.Log.e("AnthropicProvider", "Stream failed: ${response.code} isOAuth=$isOAuth body=${errorBody.take(300)}")
            throw mapHttpError(response.code, errorBody)
        }

        // Log successful request (debug builds only — see above)
        if (com.openminis.app.BuildConfig.DEBUG) {
            com.openminis.app.debug.LLMRequestLog.add(
                com.openminis.app.debug.LLMRequestLog.Entry(
                    provider = "anthropic",
                    requestURL = request.url.toString(),
                    requestHeaders = headerMap,
                    requestBody = bodyStr,
                    durationMs = durationMs,
                    responseStatusCode = response.code,
                )
            )
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        // Track current tool_use block being streamed
        var currentToolId: String? = null
        var currentToolName: String? = null
        val toolInputBuffer = StringBuilder()
        // [T-anthropic-delta-usage-merge] Usage from message_start, used to
        // backfill output-only message_delta frames.
        var startUsage: LLMUsage? = null
        // [T-anthropic-signed-thinking] index -> thinking / redacted_thinking
        // block being assembled (text + signature), in arrival order.
        val thinkingBlocks = java.util.TreeMap<Int, JSONObject>()
        val thinkingText = HashMap<Int, StringBuilder>()

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                // SSE allows `data:` with or without the space; some relays
                // emit the compact form, which used to be silently skipped.
                if (!l.startsWith("data:")) continue
                val payload = l.substring(5).trimStart()
                if (payload == "[DONE]") break

                val event = try { JSONObject(payload) } catch (_: Exception) { continue }
                // [T-android-vm-store-leak] Gated on DEBUG. This dumps the ENTIRE
                // raw SSE payload for every event — ~8.5k lines and 1.4 MB in a
                // 3-hour device log, 20% of the whole file. The `$payload`
                // interpolation builds that string BEFORE logcat decides whether
                // anything is listening, so a release build paid the allocation
                // and the I/O in full. Note the T321 summary right below this
                // deliberately logs only counts and lengths "never the actual
                // delta text — keeps log volume bounded"; this line was quietly
                // undoing that.
                //
                // NOT the cause of the stutter this was filed under (the log
                // shows the heaviest SSE hour had ZERO of the blocking GCs), but
                // it is real per-token overhead on the hot streaming path and
                // does not belong in a shipped build.
                if (com.openminis.app.BuildConfig.DEBUG) {
                    android.util.Log.d("ToolChain[Provider]", "RAW SSE: $payload")
                }
                val eventType = event.safeOptString("type", "")

                when (eventType) {
                    "message_start" -> {
                        send(LLMStreamChunk.Started)
                        event.optJSONObject("message")?.optJSONObject("usage")?.let { usage ->
                            val parsed = parseUsage(usage)
                            startUsage = parsed
                            send(LLMStreamChunk.Usage(parsed))
                        }
                    }
                    "content_block_start" -> {
                        val contentBlock = event.optJSONObject("content_block")
                        val blockIndex = event.optInt("index", -1)
                        if (contentBlock != null) when (contentBlock.safeOptString("type", "")) {
                            "thinking" -> {
                                thinkingBlocks[blockIndex] = JSONObject().apply {
                                    put("type", "thinking")
                                    put("signature", contentBlock.safeOptString("signature", ""))
                                }
                                thinkingText[blockIndex] = StringBuilder(contentBlock.safeOptString("thinking", ""))
                            }
                            "redacted_thinking" -> {
                                thinkingBlocks[blockIndex] = JSONObject().apply {
                                    put("type", "redacted_thinking")
                                    put("data", contentBlock.safeOptString("data", ""))
                                }
                            }
                        }
                        if (contentBlock?.safeOptString("type", "") == "tool_use") {
                            currentToolId = contentBlock.safeOptString("id", "")
                            currentToolName = contentBlock.safeOptString("name", "")
                            toolInputBuffer.clear()
                            android.util.Log.d("ToolChain[Provider]", "→ ToolUseStart id=$currentToolId name=$currentToolName")
                            send(LLMStreamChunk.ToolUseStart(currentToolId!!, currentToolName!!))
                        }
                    }
                    "content_block_delta" -> {
                        val delta = event.optJSONObject("delta") ?: continue
                        when (delta.safeOptString("type", "")) {
                            "text_delta" -> {
                                val text = delta.safeOptString("text", "")
                                if (text.isNotEmpty()) send(LLMStreamChunk.Text(text))
                            }
                            "thinking_delta" -> {
                                val thinking = delta.safeOptString("thinking", "")
                                thinkingText[event.optInt("index", -1)]?.append(thinking)
                                if (thinking.isNotEmpty()) send(LLMStreamChunk.ThinkingDelta(thinking))
                            }
                            "signature_delta" -> {
                                thinkingBlocks[event.optInt("index", -1)]?.let { b ->
                                    b.put("signature", b.optString("signature") + delta.safeOptString("signature", ""))
                                }
                            }
                            "input_json_delta" -> {
                                val partial = delta.safeOptString("partial_json", "")
                                if (partial.isNotEmpty() && currentToolId != null) {
                                    toolInputBuffer.append(partial)
                                    // [T-android-log-hotpath] Fires per tool-argument chunk.
                                    if (com.openminis.app.logging.AppLogger.traceEnabled) {
                                        android.util.Log.d("ToolChain[Provider]", "→ ToolInputDelta id=$currentToolId accumulated=${toolInputBuffer.length}chars")
                                    }
                                    send(LLMStreamChunk.ToolInputDelta(currentToolId!!, toolInputBuffer.toString()))
                                }
                            }
                        }
                    }
                    "content_block_stop" -> {
                        if (currentToolId != null && currentToolName != null) {
                            val args = try {
                                JSONObject(toolInputBuffer.toString())
                            } catch (_: Exception) {
                                JSONObject()
                            }
                            android.util.Log.d("ToolChain[Provider]", "→ ToolCallComplete id=$currentToolId name=$currentToolName args=${args.toString().take(300)}")
                            send(LLMStreamChunk.ToolCallComplete(currentToolId!!, currentToolName!!, args))
                            currentToolId = null
                            currentToolName = null
                            toolInputBuffer.clear()
                        }
                    }
                    "message_delta" -> {
                        event.optJSONObject("usage")?.let { usage ->
                            // [T-anthropic-delta-usage-merge] The official API
                            // repeats cumulative input/cache counts here, but many
                            // Anthropic-compatible relays send ONLY output_tokens.
                            // The view model keeps the LAST Usage chunk as the
                            // turn's usage, so an output-only frame used to wipe
                            // input/cache to 0 in the persisted stats (cache hit
                            // rate reading 0% on relays). Fill absent fields from
                            // message_start.
                            send(LLMStreamChunk.Usage(mergeDeltaUsage(startUsage, parseUsage(usage))))
                        }
                        val stopReason = event.optJSONObject("delta")
                            ?.safeOptString("stop_reason", "")?.ifEmpty { null }
                        send(LLMStreamChunk.Finished(stopReason, collectSignedThinking(thinkingBlocks, thinkingText)))
                    }
                    // [T-android-fallback-providererror] Anthropic can emit an
                    // `error` frame MID-STREAM (overloaded_error, rate-limit,
                    // an invalidated key) after tokens have already flowed.
                    // This `when` had no branch for it, so the frame was
                    // ignored, the loop ran to EOF and the flow completed
                    // NORMALLY — the turn looked successful, nothing threw, and
                    // the group-fallback decision was never reached. Throwing
                    // here routes it through the same `catch` below as any
                    // other stream failure (`cancel("Stream error", mapError(e))`),
                    // which is what carries the error to the fallback logic.
                    "error" -> {
                        val err = event.optJSONObject("error")
                        val errType = err?.safeOptString("type", "") ?: "error"
                        val errMessage = err?.safeOptString("message", "") ?: payload.take(500)
                        android.util.Log.w(
                            "AnthropicProvider",
                            "SSE error frame mid-stream: type=$errType message=${errMessage.take(200)}",
                        )
                        // Map the documented error `type` onto the same LLMError
                        // shapes mapHttpError produces for the equivalent HTTP
                        // status, so downstream classification (isFallbackable /
                        // isHttpServerError) behaves identically whether the
                        // failure arrived as a status code or as a frame.
                        throw when (errType) {
                            "overloaded_error" ->
                                LLMError.TransientError("[$errType] $errMessage", httpStatus = 529)
                            "api_error" ->
                                LLMError.TransientError("[$errType] $errMessage", httpStatus = 500)
                            "rate_limit_error" -> LLMError.RateLimited()
                            "authentication_error", "permission_error" ->
                                LLMError.InvalidApiKey("[$errType] $errMessage")
                            else -> LLMError.ProviderError("[$errType] $errMessage")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            cancel("Stream error", mapError(e))
        } finally {
            reader.close()
            response.close()
        }
        channel.close()
        awaitClose()
    }

    /**
     * [T-anthropic-wire-compat] Execute a request built by [build]. When the
     * body carried optional [WireFeature]s and the server answers 400, retry
     * once without the feature(s) the error points at (all of them when the
     * message names none). If the retry succeeds those features stay off for
     * this instance. Returns the final response (successful or not — the
     * caller maps errors as before), its body string and the request.
     */
    private fun executeWithCompatRetry(
        build: (Set<WireFeature>, MutableSet<WireFeature>?) -> JSONObject,
    ): Triple<okhttp3.Response, String, Request> {
        val used = mutableSetOf<WireFeature>()
        val suppress = disabledFeatures
        val body = build(suppress, used)
        val bodyStr = body.toString()
        val request = buildRequest(bodyStr, body)
        val response = client.newCall(request).execute()
        if (response.code != 400 || used.isEmpty()) return Triple(response, bodyStr, request)

        val errorBody = try { response.body?.string() ?: "" } catch (_: Exception) { "" }
        response.close()
        val drop = featuresBlamedBy(errorBody, used)
        android.util.Log.w(
            "AnthropicProvider",
            "400 with optional features $used; retrying without $drop. body=${errorBody.take(300)}",
        )
        val retryBody = build(suppress + drop, null)
        val retryStr = retryBody.toString()
        val retryRequest = buildRequest(retryStr, retryBody)
        val retryResponse = client.newCall(retryRequest).execute()
        if (retryResponse.isSuccessful) {
            disabledFeatures = disabledFeatures + drop
        }
        return Triple(retryResponse, retryStr, retryRequest)
    }

    /**
     * [T-anthropic-signed-thinking] Serialize the turn's replayable thinking
     * blocks (non-empty signature / data), in stream order, or null if none.
     */
    private fun collectSignedThinking(
        blocks: Map<Int, JSONObject>,
        texts: Map<Int, StringBuilder>,
    ): String? {
        val out = JSONArray()
        for ((idx, b) in blocks) {
            when (b.optString("type")) {
                "thinking" -> if (b.optString("signature").isNotEmpty()) {
                    out.put(JSONObject().apply {
                        put("type", "thinking")
                        put("thinking", texts[idx]?.toString() ?: "")
                        put("signature", b.optString("signature"))
                    })
                }
                "redacted_thinking" -> if (b.optString("data").isNotEmpty()) out.put(b)
            }
        }
        return if (out.length() == 0) null else out.toString()
    }

    /** Which of [used] a 400 body points at; all of them when unclear. */
    internal fun featuresBlamedBy(errorBody: String, used: Set<WireFeature>): Set<WireFeature> {
        val msg = errorBody.lowercase()
        val blamed = mutableSetOf<WireFeature>()
        if (WireFeature.SIGNED_THINKING_ECHO in used &&
            (msg.contains("signature") || msg.contains("redacted_thinking") ||
                msg.contains("block_binding") || msg.contains("thinking block") ||
                msg.contains("thinking` block") || msg.contains("cannot be modified"))
        ) blamed.add(WireFeature.SIGNED_THINKING_ECHO)
        if (WireFeature.EXPLICIT_THINKING_OFF in used &&
            (msg.contains("thinking.type") || msg.contains("between_tools") || msg.contains("disabled"))
        ) blamed.add(WireFeature.EXPLICIT_THINKING_OFF)
        if (WireFeature.XHIGH_EFFORT in used && (msg.contains("xhigh") || msg.contains("effort"))) {
            blamed.add(WireFeature.XHIGH_EFFORT)
        }
        return if (blamed.isEmpty()) used else blamed
    }

    /**
     * Build the `system` field as a JSON array of content blocks.
     * Mirrors iOS AnthropicProvider.resolveSystemPrompt — two authentication paths:
     *
     * - **OAuth (isOAuth=true)**: must start with the Claude Code prefix block (uncached)
     *   so Anthropic's server-side OAuth check sees the exact expected prompt. Any user
     *   tail is emitted as a second block with `cache_control: ephemeral` for cache hits.
     *   If the caller already embedded the prefix, strip it before splitting; if not,
     *   force-prepend the prefix so OAuth never fails the server-side gate.
     *
     * - **API key (isOAuth=false)**: single user-prompt block with `cache_control: ephemeral`.
     *   Returns null when the prompt is null/empty (iOS parity — no empty `system` field).
     */
    internal fun resolveSystemPrompt(userPrompt: String?): JSONArray? {
        if (isOAuth) {
            // [T-anthropic-apikey-prefix-throw] Read the (throwing) prefix ONLY
            // on the OAuth path. It used to be read before this branch, so every
            // API-key / Anthropic-compatible request on a build without the
            // secret failed with "ANTHROPIC_OAUTH_IDENTIFIER_PROMPT is not
            // configured" even though that path never sends it.
            val claudeCodePrefix = com.openminis.app.auth.ClaudeOAuthManager.ANTHROPIC_OAUTH_IDENTIFIER_PROMPT
            // Strip the prefix if the caller already prepended it; the tail is the real user prompt.
            val tail = when {
                userPrompt == null -> ""
                userPrompt.startsWith(claudeCodePrefix) ->
                    userPrompt.removePrefix(claudeCodePrefix).trimStart('\n')
                else -> userPrompt
            }
            val arr = JSONArray()
            // Block 1: Claude Code base prompt — NO cache_control (iOS parity).
            arr.put(JSONObject().apply {
                put("type", "text")
                put("text", claudeCodePrefix)
            })
            // Block 2 (optional): user tail with ephemeral cache_control for max cache hits.
            if (tail.isNotEmpty()) {
                arr.put(JSONObject().apply {
                    put("type", "text")
                    put("text", tail)
                    put("cache_control", ephemeralCacheControl())
                })
            }
            return arr
        }
        // API key path: single cached block, or null when prompt is missing/empty.
        if (userPrompt.isNullOrEmpty()) return null
        return JSONArray().put(JSONObject().apply {
            put("type", "text")
            put("text", userPrompt)
            put("cache_control", ephemeralCacheControl())
        })
    }

    private fun buildRequestBody(
        messages: List<LLMMessage>,
        systemPrompt: String?,
        maxTokens: Int,
        stream: Boolean,
        temperature: Double?,
        imageParts: List<LLMMessage.ImagePart>,
        tools: List<AgentToolDefinition> = emptyList(),
        // [T-android-thinking-level-arch] Already clamped to the model ceiling by
        // LLMProvider.streamMessage/sendMessage before reaching here.
        thinkingLevel: ThinkingLevel = ThinkingLevel.OFF,
        // [T-anthropic-wire-compat] Features to leave out of this body (sticky
        // per instance after a relay rejected them), and an out-set recording
        // which optional features the body actually carries.
        suppress: Set<WireFeature> = disabledFeatures,
        used: MutableSet<WireFeature>? = null,
    ): JSONObject {
        val body = JSONObject()
        body.put("model", model.id)
        body.put("max_tokens", maxTokens)
        body.put("stream", stream)

        if (temperature != null && !thinkingLevel.isEnabled && !modelRejectsTemperature(model.id)) {
            body.put("temperature", temperature)
        }

        // Thinking / extended thinking. Two protocol shapes:
        //   - Claude 4.6+ (adaptive): thinking.type="adaptive" + output_config.effort.
        //     The legacy budget_tokens form is silently ignored on the wire, which is
        //     the exact "thinking enabled but no thoughts" symptom seen on 4.7.
        //   - Claude <=4.5 (legacy): thinking.type="enabled" + budget_tokens. For these
        //     Anthropic also requires temperature=1 on the request.
        // Claude 4.6+ rejects temperature outright (handled by the temperature guard
        // above; we don't re-inject it here).
        // [T-thinking-rules-phase2] WHICH shape applies is decided by
        // ThinkingRuleResolver.anthropicThinkingShape — one place owns every vendor's
        // thinking contract. Emission stays here because Android's Anthropic body also
        // carries `display`/`output_config`/`temperature` companions that the abstract
        // shape deliberately does not model. Behaviour is byte-for-byte unchanged, pinned
        // by ThinkingWireGeminiAnthropicSnapshotTest.
        val thinkShape = ThinkingRuleResolver.anthropicThinkingShape(
            model.id, model.supportsReasoning, thinkingLevel, maxTokens,
        )
        com.openminis.app.logging.AppLogger.info(
            "Thinking",
            "[resolve] provider=anthropic model=${model.id} level=${thinkingLevel.name} " +
                "shape=[${thinkShape.keys.sorted().joinToString(",")}]",
        )
        // [T-anthropic-wire-compat] Whether the request ends up with thinking
        // active (explicit adaptive/enabled, or a model whose server default is
        // adaptive and that we did not turn off). Signed thinking blocks are
        // only echoed back when it is — the docs say not to mix thinking modes
        // inside one tool-use turn, and an "off" request has nothing to bind.
        var thinkingActive = false
        if (thinkingLevel.isEnabled) {
            if (modelUsesAdaptiveThinking(model.id)) {
                // [T-anthropic-thinking-display] Explicitly request summarized
                // thinking. Adaptive thinking defaults `display` to "omitted"
                // on Claude 4.7+ / all 5.x models (thinking block arrives empty,
                // signature only), so without this the UI shows no thoughts.
                body.put("thinking", JSONObject().apply {
                    put("type", "adaptive")
                    put("display", "summarized")
                })
                val effort = thinkingEffort(
                    thinkingLevel, model.id,
                    allowXhigh = WireFeature.XHIGH_EFFORT !in suppress,
                )
                if (effort == "xhigh") used?.add(WireFeature.XHIGH_EFFORT)
                body.put("output_config", JSONObject().apply {
                    put("effort", effort)
                })
                thinkingActive = true
            } else {
                val budgetTokens = thinkingBudget(maxTokens, thinkingLevel)
                // [T-anthropic-budget-floor] The API rejects budget_tokens below
                // 1024 (and it must stay < max_tokens). A tiny max_tokens (quick
                // tests, title generation) used to produce budget=max_tokens-1,
                // e.g. 127 → 400. Run without thinking instead.
                if (budgetTokens >= MIN_THINKING_BUDGET) {
                    body.put("thinking", JSONObject().apply {
                        put("type", "enabled")
                        put("budget_tokens", budgetTokens)
                    })
                    if (!modelRejectsTemperature(model.id)) {
                        body.put("temperature", 1)
                    }
                    thinkingActive = true
                }
            }
        } else {
            // [T-anthropic-thinking-off-by-model] How "thinking off" is spelled
            // differs per model (platform.claude.com thinking-troubleshooting,
            // "Thinking support, defaults, and rejected configurations"):
            //   - 4.6-4.x: think by default → must send {"type":"disabled"}
            //     (or small-maxTokens calls burn everything on thinking).
            //   - Opus 5 / Sonnet 5 / Haiku 5.5: default ON, accept "disabled".
            //     Previously we sent nothing, so OFF still thought.
            //   - Sonnet 5.5: rejects "disabled"; its off switch is
            //     {"type":"between_tools"}.
            //   - Fable / Mythos / Opus 5.5 (and unknown future ids): always on,
            //     reject "disabled". Closest to OFF is effort=low.
            //   - <= 4.5: off by default → omit.
            when (thinkingOffMode(model.id)) {
                ThinkingOffMode.DISABLED -> {
                    body.put("thinking", JSONObject().put("type", "disabled"))
                }
                ThinkingOffMode.DISABLED_5X -> {
                    if (WireFeature.EXPLICIT_THINKING_OFF !in suppress) {
                        body.put("thinking", JSONObject().put("type", "disabled"))
                        used?.add(WireFeature.EXPLICIT_THINKING_OFF)
                    } else {
                        body.put("output_config", JSONObject().put("effort", "low"))
                        thinkingActive = true
                    }
                }
                ThinkingOffMode.BETWEEN_TOOLS -> {
                    if (WireFeature.EXPLICIT_THINKING_OFF !in suppress) {
                        body.put("thinking", JSONObject().put("type", "between_tools"))
                        used?.add(WireFeature.EXPLICIT_THINKING_OFF)
                    } else {
                        body.put("output_config", JSONObject().put("effort", "low"))
                        thinkingActive = true
                    }
                }
                ThinkingOffMode.LOW_EFFORT -> {
                    body.put("output_config", JSONObject().put("effort", "low"))
                    thinkingActive = true
                }
                ThinkingOffMode.OMIT -> Unit
            }
        }

        // System prompt blocks — mirrors iOS AnthropicProvider.resolveSystemPrompt.
        val systemArray = resolveSystemPrompt(systemPrompt)
        if (systemArray != null) {
            body.put("system", systemArray)
        }

        // Tools: enable eager_input_streaming on every tool (GA feature, no beta
        // header required) so the SDK streams partial tool input JSON as it's
        // generated — mirrors iOS `RequestBodyPatcher.injectEagerInputStreaming`.
        // cache_control: ephemeral goes on the *last* tool, matching the first of
        // the four prompt-cache breakpoints Anthropic honors.
        if (tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            for ((i, tool) in tools.withIndex()) {
                val toolJson = tool.toAnthropicJson()
                toolJson.put("eager_input_streaming", true)
                if (i == tools.lastIndex) {
                    toolJson.put("cache_control", ephemeralCacheControl())
                }
                toolsArray.put(toolJson)
            }
            body.put("tools", toolsArray)
            body.put("tool_choice", JSONObject().put("type", "auto"))
        }

        // Build and merge messages
        // [T-anthropic-signed-thinking] Replay signed thinking blocks on
        // Claude models while thinking is active (docs: "when you return tool
        // results, you must pass the thinking blocks from the assistant
        // message back to the API, complete and unmodified").
        val echoSigned = thinkingActive &&
            WireFeature.SIGNED_THINKING_ECHO !in suppress &&
            parseClaudeVersion(model.id) != null &&
            !shouldEchoInterleavedThinking()
        val rawMessages = buildMessages(messages, imageParts, echoSigned, used)
        val merged = mergeConsecutiveSameRole(rawMessages)
        ensureEndsWithUserTurn(merged)

        // Inject cache_control on last 2 user messages
        injectMessageCacheControl(merged)

        body.put("messages", merged)

        return body
    }

    /**
     * Drop `tool_result` parts whose `tool_use_id` does not match a `tool_use`
     * in the most recent assistant message. Anthropic rejects orphan
     * tool_results with 400 (`unexpected tool_use_id ... no corresponding
     * tool_use block`). Operates on a copy so the caller's stored history
     * (DB rows / in-memory state) is untouched — only the outbound payload
     * is sanitized. Mirrors iOS AnthropicAgentProvider.stripOrphanToolResults.
     */
    private fun stripOrphanToolResults(messages: List<LLMMessage>): List<LLMMessage> {
        var liveToolUseIds: Set<String> = emptySet()
        val result = ArrayList<LLMMessage>(messages.size)
        for ((i, msg) in messages.withIndex()) {
            when (msg.role) {
                LLMMessage.Role.ASSISTANT -> {
                    liveToolUseIds = msg.contentParts
                        .filterIsInstance<AgentContentPart.ToolUse>()
                        .map { it.id }
                        .toSet()
                    result.add(msg)
                }
                LLMMessage.Role.USER -> {
                    val original = msg.contentParts
                    if (original.isEmpty()) {
                        result.add(msg)
                    } else {
                        val kept = original.filter { part ->
                            if (part is AgentContentPart.ToolResult) {
                                liveToolUseIds.contains(part.id)
                            } else true
                        }
                        if (kept.size != original.size) {
                            val dropped = original.size - kept.size
                            android.util.Log.i(
                                "AnthropicProvider",
                                "Stripped $dropped orphan tool_result block(s) from outbound payload (idx=$i)"
                            )
                            result.add(msg.copy(contentParts = kept))
                        } else {
                            result.add(msg)
                        }
                    }
                    // Clear so a later user turn can't match a stale assistant.
                    liveToolUseIds = emptySet()
                }
            }
        }
        // Drop user messages that became empty (only orphan tool_results).
        // An empty content array is itself a 400.
        return result.filter { m ->
            // Keep messages that still have either non-empty contentParts or a
            // non-empty `content` string (string-only messages never had parts
            // to strip in the first place).
            m.contentParts.isNotEmpty() || m.content.isNotEmpty()
        }
    }

    /**
     * [T-android-anthropic-thinking-echo] (issue #70) Whether to echo prior
     * assistant `thinking` content blocks back into the history.
     *
     * Anthropic-compat proxies that speak the Anthropic protocol but reason
     * with interleaved (unsigned) thinking — DeepSeek V4 / GLM-4.5+ / Kimi
     * K2.5+ / Nemotron etc. — reject a multi-turn tool-use history where a
     * previous assistant turn produced thinking but that block isn't passed
     * back: 400 `content[].thinking in the thinking mode must be passed back
     * to the API`. The captured reasoning already lives on
     * `LLMMessage.reasoningContent`; we replay it as a synthesized
     * `{"type":"thinking","thinking":…}` block (no `signature` — we never
     * capture one off the wire).
     *
     * Mirrors iOS AnthropicAgentProvider's `echoReasoning` gate exactly:
     *   echo = supportsReasoning != false        (model may reason)
     *          AND interleavedReasoningField != null  (needs + tolerates
     *              UNSIGNED interleaved echo)
     *          AND !official Anthropic endpoint.
     * The `interleavedReasoningField` gate is load-bearing: the real
     * api.anthropic.com and strict Claude-class proxies verify the
     * `signature` field and reject an unsigned thinking block with
     * `thinking.signature: invalid_request_error`, so we must NOT echo there
     * even when supportsReasoning=true. Independent of the user's thinking
     * toggle — DeepSeek V4 emits thinking even when we don't request it.
     */
    private fun shouldEchoInterleavedThinking(): Boolean {
        val modelMayReason = model.supportsReasoning != false
        val modelRequiresInterleave = model.interleavedReasoningField != null
        return modelMayReason && modelRequiresInterleave && isCustomEndpoint
    }

    /** Build the messages JSONArray from LLMMessage list. */
    private fun buildMessages(
        rawMessages: List<LLMMessage>,
        imageParts: List<LLMMessage.ImagePart>,
        echoSigned: Boolean = false,
        used: MutableSet<WireFeature>? = null,
    ): JSONArray {
        // Sanitize orphan tool_results before serialization. Operates on a
        // copy — caller's stored history is untouched.
        val messages = stripOrphanToolResults(rawMessages)
        // [T-android-anthropic-thinking-echo] (issue #70) Compute once — the
        // gate depends only on the model + endpoint, not the message.
        val echoThinking = shouldEchoInterleavedThinking()
        val messagesArray = JSONArray()
        for ((index, msg) in messages.withIndex()) {
            val obj = JSONObject()
            obj.put("role", msg.role.value)
            // [T-anthropic-signed-thinking] Signed thinking / redacted_thinking
            // blocks captured off the wire for this assistant turn, replayed
            // verbatim ahead of its text / tool_use.
            val signed = if (echoSigned && msg.role == LLMMessage.Role.ASSISTANT) {
                parseSignedThinkingBlocks(msg.anthropicThinkingBlocks)
            } else emptyList()
            if (signed.isNotEmpty()) used?.add(WireFeature.SIGNED_THINKING_ECHO)

            if (msg.contentParts.isNotEmpty()) {
                val contentArray = JSONArray()
                // [T-android-anthropic-thinking-echo] (issue #70) Prepend the
                // synthesized thinking block FIRST so it precedes text/tool_use
                // (Anthropic requires the thinking block to lead the assistant
                // turn). Replay captured reasoning verbatim; fall back to an
                // empty string as a field-presence placeholder for turns whose
                // reasoning was never captured (interrupted stream, history from
                // before thinking was on, cross-provider session) — without it
                // the compat proxy still 400s. Mirrors iOS
                // injectThinkingBlocksForCompatProxy (unsigned block, no
                // signature). User turns never carry thinking.
                if (echoThinking && msg.role == LLMMessage.Role.ASSISTANT) {
                    contentArray.put(JSONObject().apply {
                        put("type", "thinking")
                        put("thinking", msg.reasoningContent ?: "")
                    })
                }
                for (block in signed) contentArray.put(block)
                for (part in msg.contentParts) {
                    when (part) {
                        is AgentContentPart.Text -> {
                            // [T-anthropic-empty-text-block] The API 400s on
                            // `text content blocks must be non-empty`; an empty
                            // part carries no information, so drop it.
                            if (part.text.isNotEmpty()) {
                                contentArray.put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", part.text)
                                })
                            }
                        }
                        is AgentContentPart.ToolUse -> {
                            // Anthropic requires tool ids to match ^[a-zA-Z0-9_-]+$.
                            // OpenAI Responses can emit ids like `call_abc|fc_def`
                            // (pipe), so echo every id through sanitizeToolId.
                            contentArray.put(JSONObject().apply {
                                put("type", "tool_use")
                                put("id", sanitizeToolId(part.id))
                                put("name", part.name)
                                put("input", part.input)
                            })
                        }
                        is AgentContentPart.ToolResult -> {
                            val resultContent = JSONArray()
                            // [T-anthropic-empty-text-block] An empty tool
                            // output (e.g. a silent command) used to emit
                            // `{"type":"text","text":""}` → 400. Use a stable
                            // placeholder instead (stable so the cached prefix
                            // replays byte-identically).
                            resultContent.put(JSONObject().apply {
                                put("type", "text")
                                put("text", part.content.ifEmpty { "(empty)" })
                            })
                            if (part.imageData != null && part.imageMimeType != null) {
                                // T-imgsize: belt-and-braces — history tool-result
                                // screenshots can be raw 12-megapixel PNGs that
                                // never went through the composer's budget pass.
                                // Re-encode in-place if a single part already blows
                                // the per-image cap so we don't 413 on replay.
                                resultContent.put(imageBlock(part.imageData, part.imageMimeType))
                            }
                            contentArray.put(JSONObject().apply {
                                put("type", "tool_result")
                                put("tool_use_id", sanitizeToolId(part.id))
                                put("content", resultContent)
                                if (part.isError) put("is_error", true)
                            })
                        }
                        is AgentContentPart.ImageData -> {
                            // T-imgsize: provider-boundary backstop for history
                            // image bytes that bypassed the composer budget
                            // (restored sessions, retry-after-edit, agent-emitted
                            // ImageData). Composer already capped fresh sends.
                            contentArray.put(imageBlock(part.data, part.mimeType))
                        }
                    }
                }
                obj.put("content", contentArray)
            } else {
                val isLastUser = msg.role == LLMMessage.Role.USER &&
                    index == messages.indexOfLast { it.role == LLMMessage.Role.USER }
                if (isLastUser && imageParts.isNotEmpty()) {
                    val contentArray = JSONArray()
                    for (part in imageParts) {
                        // T-imgsize: backstop for the legacy ImagePart path used
                        // when contentParts is empty (older user-message shape
                        // pre-T132). Composer already runs ImageBudget but this
                        // arm is also reached on session restore where
                        // contentParts may not be populated.
                        contentArray.put(imageBlock(part.data, part.mimeType))
                    }
                    if (msg.content.isNotEmpty()) {
                        contentArray.put(JSONObject().apply {
                            put("type", "text")
                            put("text", msg.content)
                        })
                    }
                    obj.put("content", contentArray)
                } else if (echoThinking && msg.role == LLMMessage.Role.ASSISTANT) {
                    // [T-android-anthropic-thinking-echo] (issue #70) Assistant
                    // turn with no structured parts (plain-text reply, or history
                    // rehydrated without contentParts). Still must carry its
                    // thinking block, so materialize the string content into an
                    // array with the thinking block first.
                    val contentArray = JSONArray()
                    contentArray.put(JSONObject().apply {
                        put("type", "thinking")
                        put("thinking", msg.reasoningContent ?: "")
                    })
                    if (msg.content.isNotEmpty()) {
                        contentArray.put(JSONObject().apply {
                            put("type", "text")
                            put("text", msg.content)
                        })
                    }
                    obj.put("content", contentArray)
                } else if (signed.isNotEmpty()) {
                    val contentArray = JSONArray()
                    for (block in signed) contentArray.put(block)
                    if (msg.content.isNotEmpty()) {
                        contentArray.put(JSONObject().apply {
                            put("type", "text")
                            put("text", msg.content)
                        })
                    }
                    obj.put("content", contentArray)
                } else {
                    obj.put("content", msg.content)
                }
            }

            // [T-anthropic-empty-text-block] After dropping empty text parts a
            // message can end up with no content at all (`[]` or `""`), which is
            // its own 400. Skip it; mergeConsecutiveSameRole then joins the
            // neighbours so roles still alternate.
            val c = obj.opt("content")
            val isEmpty = (c is JSONArray && c.length() == 0) || (c is String && c.isEmpty())
            if (isEmpty) continue
            messagesArray.put(obj)
        }
        return messagesArray
    }

    /**
     * Merge consecutive messages with the same role.
     * Anthropic API requires alternating user/assistant messages.
     * For assistant messages, reorder: text/image first, then tool_use (Anthropic
     * rejects text appearing after tool_use in the same message).
     */
    /**
     * [T-android-image-upload-format] One inline image as an Anthropic content
     * block. Format, byte budget and `media_type` all come from
     * [ImageBudget.prepareForUpload], so the label always matches the bytes;
     * an image that cannot be made sendable becomes a text block instead of a
     * request the API rejects on every retry.
     */
    private fun imageBlock(data: ByteArray, declaredMime: String?): JSONObject {
        val upload = ImageBudget.prepareForUpload(data, declaredMime)
            ?: return JSONObject().apply {
                put("type", "text")
                put("text", ImageBudget.UNSENDABLE_IMAGE_PLACEHOLDER)
            }
        return JSONObject().apply {
            put("type", "image")
            put("source", JSONObject().apply {
                put("type", "base64")
                put("media_type", upload.mimeType)
                put("data", upload.base64())
            })
        }
    }

    private fun mergeConsecutiveSameRole(messages: JSONArray): JSONArray {
        if (messages.length() <= 1) return messages
        val result = JSONArray()
        var i = 0
        while (i < messages.length()) {
            val current = messages.getJSONObject(i)
            val role = current.getString("role")
            // Collect all consecutive messages with same role
            val contentBlocks = mutableListOf<JSONObject>()
            collectContentBlocks(current, contentBlocks)
            var j = i + 1
            while (j < messages.length() && messages.getJSONObject(j).getString("role") == role) {
                collectContentBlocks(messages.getJSONObject(j), contentBlocks)
                j++
            }
            if (j > i + 1 || contentBlocks.isNotEmpty()) {
                // Reorder assistant content: thinking first, then text/image,
                // then tool_use last. Anthropic rejects text before thinking and
                // text after tool_use in the same turn; when several assistant
                // messages merge (T-android-anthropic-thinking-echo, issue #70)
                // this hoists every synthesized thinking block ahead of the text
                // so the merged turn still leads with thinking.
                if (role == "assistant") {
                    val thinking = contentBlocks.filter { isThinkingBlock(it) }
                    val other = contentBlocks.filter {
                        !isThinkingBlock(it) && it.optString("type") != "tool_use"
                    }
                    val toolUse = contentBlocks.filter { it.optString("type") == "tool_use" }
                    contentBlocks.clear()
                    contentBlocks.addAll(thinking)
                    contentBlocks.addAll(other)
                    contentBlocks.addAll(toolUse)
                }
                val merged = JSONObject().apply {
                    put("role", role)
                    put("content", JSONArray(contentBlocks.map { it }))
                }
                result.put(merged)
            } else {
                result.put(current)
            }
            i = j
        }
        return result
    }

    /** Extract content blocks from a message into a flat list. */
    private fun collectContentBlocks(msg: JSONObject, out: MutableList<JSONObject>) {
        val content = msg.opt("content")
        when (content) {
            is JSONArray -> {
                for (k in 0 until content.length()) {
                    out.add(content.getJSONObject(k))
                }
            }
            is String -> {
                out.add(JSONObject().apply {
                    put("type", "text")
                    put("text", content)
                })
            }
        }
    }

    /**
     * Place the two message-level cache breakpoints (system + tools use the
     * other two of Anthropic's four).
     *
     * [T-anthropic-cache-breakpoints] Two bugs in the previous version:
     *
     *  1. **String-content user messages got NO breakpoint.** A plain text turn
     *     (no attachments, no tool results) serialises as `"content": "…"`, and
     *     the old code only marked `content is JSONArray`. In an ordinary chat
     *     every user turn is a string, so the only cached segment was
     *     system+tools and the whole history was re-billed at full input price
     *     every turn. Strings are now promoted to a single text block so the
     *     marker can attach.
     *
     *  2. **Breakpoints skipped thinking/empty blocks badly.** Anthropic rejects
     *     `cache_control` on `thinking` / `redacted_thinking` blocks and on
     *     empty text blocks; the marker now walks back to the last cacheable
     *     block of the message.
     *
     * Placement stays "last two user messages": the newest one caches the full
     * prefix for the NEXT request in the agent loop (whose new tail is the
     * assistant tool_use + next tool_result), and the previous one is the
     * read point that this request hits — the 20-block lookback then covers a
     * normal tool round without needing a third marker.
     */
    private fun injectMessageCacheControl(messages: JSONArray) {
        var userCount = 0
        for (i in messages.length() - 1 downTo 0) {
            val msg = messages.getJSONObject(i)
            if (msg.getString("role") != "user") continue
            val content = msg.opt("content")
            val blocks: JSONArray = when (content) {
                is JSONArray -> content
                is String -> {
                    if (content.isEmpty()) continue
                    JSONArray().put(JSONObject().put("type", "text").put("text", content))
                        .also { msg.put("content", it) }
                }
                else -> continue
            }
            var marked = false
            for (k in blocks.length() - 1 downTo 0) {
                val block = blocks.optJSONObject(k) ?: continue
                if (isCacheableBlock(block)) {
                    block.put("cache_control", ephemeralCacheControl())
                    marked = true
                    break
                }
            }
            if (marked) {
                userCount++
                if (userCount >= 2) break
            }
        }
    }

    /** Blocks Anthropic accepts a `cache_control` marker on. */
    private fun isCacheableBlock(block: JSONObject): Boolean = when (block.optString("type")) {
        "thinking", "redacted_thinking" -> false
        "text" -> block.optString("text").isNotEmpty()
        else -> true
    }

    private fun isThinkingBlock(block: JSONObject): Boolean =
        when (block.optString("type")) {
            "thinking", "redacted_thinking" -> true
            else -> false
        }

    /**
     * [T-anthropic-signed-thinking] Decode the blocks stored on
     * [LLMMessage.anthropicThinkingBlocks]. Only blocks that can be replayed
     * are returned: `thinking` with a non-empty `signature`, or
     * `redacted_thinking` with non-empty `data` — an empty/truncated signature
     * is a hard 400 ("Invalid `signature` in `thinking` block").
     */
    private fun parseSignedThinkingBlocks(raw: String?): List<JSONObject> {
        if (raw.isNullOrEmpty()) return emptyList()
        val arr = try { JSONArray(raw) } catch (_: Exception) { return emptyList() }
        val out = ArrayList<JSONObject>(arr.length())
        for (i in 0 until arr.length()) {
            val b = arr.optJSONObject(i) ?: continue
            when (b.optString("type")) {
                "thinking" -> if (b.optString("signature").isNotEmpty()) {
                    out.add(JSONObject().apply {
                        put("type", "thinking")
                        put("thinking", b.optString("thinking"))
                        put("signature", b.optString("signature"))
                    })
                }
                "redacted_thinking" -> if (b.optString("data").isNotEmpty()) {
                    out.add(JSONObject().apply {
                        put("type", "redacted_thinking")
                        put("data", b.optString("data"))
                    })
                }
            }
        }
        return out
    }

    /**
     * [T-anthropic-no-prefill] Claude 4.6+ reject a request whose last message
     * is an assistant turn ("This model does not support assistant message
     * prefill. The conversation must end with a user message."), and earlier
     * models reject it while thinking is on. Minis never prefills on purpose;
     * a trailing assistant only appears when sanitizing removed the final user
     * turn (orphan tool_results / empty content). Close it with a minimal user
     * turn instead of sending a request that can only fail.
     */
    private fun ensureEndsWithUserTurn(messages: JSONArray) {
        if (messages.length() == 0) return
        val last = messages.getJSONObject(messages.length() - 1)
        if (last.optString("role") != "assistant") return
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", JSONArray().put(JSONObject().apply {
                put("type", "text")
                put("text", "Continue.")
            }))
        })
    }

    companion object {
        /**
         * Parse the `-<major>-<minor>` (or `/<major>.<minor>`) version out of a Claude id.
         * [T-anthropic-temp-claude5-android] The minor segment is OPTIONAL and
         * defaults to 0: single-segment ids (claude-fable-5, claude-opus-5,
         * future claude-*-6) parse as (major, 0). Previously they returned
         * null, so modelRejectsTemperature said false and the API answered
         * 400 "temperature is deprecated for this model"; the same null also
         * blinded modelUsesAdaptiveThinking and supportsThinking for the
         * whole 5-series. The optional group is greedy, so a real pair like
         * claude-3-5-sonnet still wins as (3,5) — never (3,0).
         * Returns null for non-Claude ids or ids without any version digits.
         *
         * TWO POSITIONAL BUGS FIXED (mirrored on iOS AnthropicProvider.swift):
         *
         * 1. The minor group is capped at 1-2 digits. It was uncapped and
         *    greedy, so a SINGLE-segment version followed by a snapshot stamp
         *    swallowed the stamp: `claude-sonnet-4-20250514` (Claude Sonnet 4.0)
         *    parsed as (4, 20250514) and was therefore treated as >= 4.6 —
         *    temperature dropped, adaptive effort sent, and
         *    `thinking.type=disabled` sent, none of which a 4.0 model accepts.
         *    Two shipping catalog ids hit this (claude-sonnet-4-20250514,
         *    claude-opus-4-20250514, plus their `anthropic/` forms). No real
         *    Claude version has ever had a 3+ digit minor, and a version PAIR is
         *    unaffected either way (`claude-opus-4-8-20260115` -> (4,8)) because
         *    the pair consumes the separator first.
         *
         * 2. The search STARTS AFTER the word "claude". It used to take the
         *    first version-shaped run anywhere in the id, so a namespace
         *    carrying digits before the family name won: `v2-gateway/claude-opus-5`
         *    parsed as (2, 0), and `inst-53/claude-haiku-4-5` as (53, 0). Both
         *    would then take `temperature` plus legacy budget thinking — the
         *    exact 400 and silent no-op this commit family exists to prevent.
         *    Only digits AFTER the family name can be the version.
         */
        private fun parseClaudeVersion(modelId: String): Pair<Int, Int>? {
            val lower = modelId.lowercase()
            val claudeAt = lower.indexOf("claude")
            if (claudeAt < 0) return null
            val afterClaude = lower.substring(claudeAt + "claude".length)
            val regex = Regex("""[-/]?(\d+)(?:[-.](\d{1,2}))?(?:$|[^0-9])""")
            val match = regex.find(afterClaude) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: 0
            return major to minor
        }

        /**
         * Claude models from 4.6 onward reject the `temperature` parameter.
         * Returns true when the model id parses as Claude version >= 4.6.
         */
        fun modelRejectsTemperature(modelId: String): Boolean {
            val (major, minor) = parseClaudeVersion(modelId) ?: return false
            return major > 4 || (major == 4 && minor >= 6)
        }

        /**
         * Claude 4.6 and later use *adaptive* thinking (`thinking.type="adaptive"` plus
         * `output_config.effort = low|medium|high|xhigh|max`) and silently ignore the
         * older `thinking.type="enabled" + budget_tokens` form. Older Claude models still
         * need the legacy budget-based form.
         */
        fun modelUsesAdaptiveThinking(modelId: String): Boolean {
            val (major, minor) = parseClaudeVersion(modelId) ?: return false
            return major > 4 || (major == 4 && minor >= 6)
        }

        /**
         * [T-android-claude5-thinking-disabled-400] Whether this model accepts an
         * explicit `thinking: {"type": "disabled"}` on the wire. Ported from iOS
         * `AnthropicProvider.modelAcceptsExplicitThinkingDisabled` (69be65763).
         *
         * Claude 4.6-4.x adaptive models DO: they think by default when the field
         * is absent, so "thinking off" has to be stated, or a small-maxTokens call
         * burns its whole budget on thinking and returns empty text (the reason the
         * disabled literal exists at all).
         *
         * Claude 5+ does NOT. The API rejects the value outright:
         *
         *     [invalid_request_error] "thinking.type.disabled" is not supported
         *     for this model. Thinking defaults to adaptive mode when not
         *     specified; use "thinking.type.enabled" with "budget_tokens" for
         *     extended thinking.
         *
         * So on 5+ the correct encoding of "off" is to send NO thinking field —
         * the server's adaptive default handles it. Reported on iOS against fable 5
         * (Claude Opus 5, official OAuth) where every non-thinking message 400'd;
         * Android had the same conflation and the same exposure for claude-sonnet-5.
         *
         * This is the split iOS made: `modelUsesAdaptiveThinking` answers "does
         * this model think by default / take an effort tier" (4.6+ AND 5+), while
         * this predicate answers "may we send thinking.type=disabled" (4.6-4.x
         * only). They are NOT the same question.
         *
         * Deliberately keyed on the parsed version rather than a model allowlist:
         * this is a generation-wide API contract change, and an allowlist would
         * 400 again on the next Claude 5 variant nobody remembered to add.
         */
        fun modelAcceptsExplicitThinkingDisabled(modelId: String): Boolean =
            when (thinkingOffMode(modelId)) {
                ThinkingOffMode.DISABLED, ThinkingOffMode.DISABLED_5X -> true
                else -> false
            }

        /** API minimum for legacy `budget_tokens` (extended-thinking docs). */
        const val MIN_THINKING_BUDGET = 1024

        /** How a thinking-OFF request is encoded for a given model. */
        enum class ThinkingOffMode {
            /** Send nothing: model is off by default (<= 4.5) or not Claude. */
            OMIT,
            /** `{"type":"disabled"}` — Claude 4.6-4.x (on/adaptive by default). */
            DISABLED,
            /** `{"type":"disabled"}` — Claude Opus 5, Sonnet 5, Haiku 5.5. */
            DISABLED_5X,
            /** `{"type":"between_tools"}` — Claude Sonnet 5.5 only. */
            BETWEEN_TOOLS,
            /** Thinking can't be turned off; lower `effort` to "low" instead. */
            LOW_EFFORT,
        }

        /** Family word of a Claude id (opus / sonnet / haiku / fable / mythos). */
        internal fun claudeFamily(modelId: String): String? =
            Regex("(opus|sonnet|haiku|fable|mythos)").find(modelId.lowercase())?.value

        /**
         * [T-anthropic-thinking-off-by-model] Source: platform.claude.com
         * "Troubleshooting thinking" → per-model table. Opus 5, Sonnet 5 and
         * Haiku 5.5 accept `disabled` (at effort <= high, which is what an OFF
         * request runs at); Sonnet 5.5 takes `between_tools`; Fable 5 / 5.1,
         * Mythos 5 / 5.1 and Opus 5.5 are always-on. Unknown future 5+/6+ ids
         * default to the always-on handling (effort only), which no adaptive
         * model rejects.
         */
        fun thinkingOffMode(modelId: String): ThinkingOffMode {
            val (major, minor) = parseClaudeVersion(modelId) ?: return ThinkingOffMode.OMIT
            if (major < 4 || (major == 4 && minor < 6)) return ThinkingOffMode.OMIT
            if (major == 4) return ThinkingOffMode.DISABLED
            val family = claudeFamily(modelId)
            if (major == 5) {
                when {
                    minor == 0 && (family == "opus" || family == "sonnet") -> return ThinkingOffMode.DISABLED_5X
                    minor == 5 && family == "haiku" -> return ThinkingOffMode.DISABLED_5X
                    minor == 5 && family == "sonnet" -> return ThinkingOffMode.BETWEEN_TOOLS
                }
            }
            return ThinkingOffMode.LOW_EFFORT
        }

        /**
         * `xhigh` effort exists on Claude 4.7+ (Opus 4.7 / 4.8 and every 5.x);
         * Opus 4.6 / Sonnet 4.6 top out at `max` without `xhigh`.
         */
        fun modelSupportsXhighEffort(modelId: String): Boolean {
            val (major, minor) = parseClaudeVersion(modelId) ?: return false
            return major > 4 || (major == 4 && minor >= 7)
        }

        /**
         * [T-android-claude-opus48-thinking-toggle] (Sow Sow 38845/38850) True
         * when this Claude model supports extended thinking — i.e. the Deep
         * Thinking toggle should appear for it. Anthropic added extended thinking
         * with Claude 3.7; every 4.x model (opus / sonnet / haiku) supports it.
         * Used to stamp `supportsReasoning = true` on models fetched from
         * `/v1/models` (which returns no capability metadata), so the toggle shows
         * on a direct-Anthropic instance even when models.dev hasn't catalogued
         * the model yet (e.g. brand-new Opus 4.8). The provider already builds the
         * correct `thinking` request for any such model (see
         * modelUsesAdaptiveThinking / the legacy budget path), so this is purely
         * the UI-capability flag those request paths assumed.
         */
        fun supportsThinking(modelId: String): Boolean {
            val (major, minor) = parseClaudeVersion(modelId) ?: return false
            return major > 4 || major == 4 || (major == 3 && minor >= 7)
        }

        /** Calculate thinking budget tokens based on level (legacy <=4.5 protocol). */
        fun thinkingBudget(maxTokens: Int, level: ThinkingLevel): Int {
            // Below the API floor there is no valid budget at all.
            if (maxTokens <= MIN_THINKING_BUDGET) return 0
            val cap = when (level) {
                ThinkingLevel.OFF -> 0
                ThinkingLevel.LOW -> 8192
                ThinkingLevel.MEDIUM -> 32768
                ThinkingLevel.HIGH -> minOf(maxTokens, 65536)
                // [T-android-thinking-level-arch] XHIGH and above all take the
                // full token budget (already capped to maxTokens below).
                ThinkingLevel.XHIGH,
                ThinkingLevel.MAX,
                ThinkingLevel.ULTRA -> maxTokens
            }
            // Anthropic requires budget_tokens STRICTLY LESS THAN max_tokens; an
            // equal value is a 400 ("thinking.budget_tokens must be less than
            // max_tokens"). HIGH's min(maxTokens, 65536) and the top tiers both
            // clamp to exactly maxTokens, so pull back to maxTokens-1 when they
            // hit the ceiling. Affects the legacy budget_tokens path (pre-4.6
            // Claude + Anthropic-compatible relays without adaptive thinking).
            val clamped = minOf(cap, maxTokens)
            return if (clamped >= maxTokens && maxTokens > 1) maxTokens - 1 else clamped
        }

        /**
         * Map ThinkingLevel onto Anthropic adaptive thinking effort strings (Claude 4.6+).
         * `xhigh` maps to "max" — both 4.6 and 4.7 advertise "max" universally; 4.7 also
         * lists "xhigh" but we standardize on "max" for cross-version compatibility.
         */
        fun thinkingEffort(level: ThinkingLevel, modelId: String, allowXhigh: Boolean = true): String =
            if (level == ThinkingLevel.XHIGH && allowXhigh && modelSupportsXhighEffort(modelId)) "xhigh"
            else thinkingEffort(level)

        fun thinkingEffort(level: ThinkingLevel): String = when (level) {
            ThinkingLevel.OFF -> "low"  // unreached: caller checks isEnabled
            ThinkingLevel.LOW -> "low"
            ThinkingLevel.MEDIUM -> "medium"
            ThinkingLevel.HIGH -> "high"
            // [T-android-thinking-level-arch] Anthropic tops out at "max"; XHIGH
            // and the new MAX/ULTRA tiers all collapse onto it.
            ThinkingLevel.XHIGH,
            ThinkingLevel.MAX,
            ThinkingLevel.ULTRA -> "max"
        }
    }

    /**
     * T302: takes the pre-serialized body string AND the JSONObject. The
     * string is used directly for the OkHttp RequestBody so we don't pay
     * for a second `body.toString()` (each call is tens of MB on long
     * agent loops). The JSONObject is still needed for header logic that
     * inspects fields like `thinking`. See OpenAIProvider.buildRequest
     * for the same pattern.
     */
    private fun buildRequest(bodyStr: String, body: JSONObject): Request {
        // T192: `basePath` may already end in `/v1` because
        // `ProviderInstance.effectiveBaseURL` appends `/v1` when
        // `appendV1Suffix=true` and the user-entered base doesn't end in `/v1`.
        // Without stripping here we'd produce `…/v1/v1/messages` for Anthropic-
        // compatible endpoints (DeepSeek, Bailian, etc.). Mirrors
        // `AnthropicProvider.swift::stripV1Suffix` (iOS) so the same custom
        // base URL works on both platforms.
        val cleanBase = basePath.trimEnd('/').let {
            if (it.endsWith("/v1")) it.dropLast(3).trimEnd('/') else it
        }
        val builder = Request.Builder()
            .url("$cleanBase/v1/messages")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")

        // OkHttp forbids multiple values for the same header slot via `.header(...)`,
        // so build a combined anthropic-beta string and set it once.
        //
        // OAuth (Claude Code) credentials are served with the Claude CLI's
        // request profile — the anthropic-beta set, User-Agent and X-Stainless-*
        // headers. Without it some features (e.g. thinking on 4.7) are not
        // enabled for these credentials. Values tracked from Wei-Shaw/sub2api
        // backend/internal/pkg/claude/constants.go.
        //
        // For API-key / custom endpoints we only carry the betas actually needed
        // by the request body; we must NOT include oauth-2025-04-20 or
        // claude-code-20250219 there because those signal Claude-Code-only
        // surface and get rejected on plain API-key auth.
        val betaFlags = mutableListOf<String>()
        if (isOAuth) {
            // [T-anthropic-redact-thinking] Deliberately OMIT
            // "redact-thinking-2026-02-12" from the Claude Code OAuth betas.
            // When present, Anthropic redacts the plaintext of `thinking` content
            // blocks (returns an empty `thinking` string with only a `signature`),
            // so a reasoning model runs (usage.thinking_tokens > 0) but the App
            // can't show any thinking text. The official Claude Code CLI only adds
            // this beta when `showThinkingSummaries` is unset/false (per
            // anthropics/claude-code#31326 and the
            // code.claude.com model-config docs); omitting it is equivalent to
            // `showThinkingSummaries: true` — pure UI visibility, no effect on
            // reasoning quality or token budget. All other OAuth betas stay.
            betaFlags.addAll(listOf(
                "claude-code-20250219",
                "oauth-2025-04-20",
                "interleaved-thinking-2025-05-14",
                "prompt-caching-scope-2026-01-05",
                "effort-2025-11-24",
                "context-management-2025-06-27",
                "extended-cache-ttl-2025-04-11",
            ))
        } else if (body.has("thinking")) {
            val isAdaptive = body.optJSONObject("thinking")?.optString("type") == "adaptive"
            if (isAdaptive) {
                betaFlags.add("effort-2025-11-24")
            } else {
                betaFlags.add("interleaved-thinking-2025-05-14")
            }
        }
        // [T-android-enhanced-cache] API-key path only carries the betas the
        // body needs; the 1-hour cache TTL requires this flag. OAuth already
        // lists it above, so guard on !isOAuth to avoid a duplicate token.
        if (enhancedCache && !isOAuth) {
            betaFlags.add("extended-cache-ttl-2025-04-11")
        }
        if (betaFlags.isNotEmpty()) {
            builder.header("anthropic-beta", betaFlags.joinToString(","))
        }

        // Stainless / CLI fingerprint headers — only on OAuth; bump in lockstep
        // with sub2api when the real CLI version moves.
        //
        // [T-anthropic-fable51-android] 2.1.195 -> 2.1.251. Anthropic gates
        // models on this version: below 2.1.251 a claude-fable-5-1 request is
        // refused with "Claude Code <ver> does not support this model; version
        // 2.1.251 or newer is required." 2.1.251 is the value CLIProxyAPI
        // adopted for exactly this reason (router-for-me/CLIProxyAPI#5405,
        // bumping their defaultClaudeFingerprintUserAgent from 2.1.220), not a
        // number picked to clear the error message.
        //
        // [T-anthropic-opus55-catalog] 2.1.251 -> 2.1.280, for the same class
        // of gate one generation later: Claude Opus 5.5 is refused below
        // 2.1.280, and the refusal is a plain 400 that reads like a bad model
        // id rather than a version problem. The floor only ever rises, so
        // every model that worked at 2.1.251 (Fable 5.1, Sonnet/Opus 4.x, …)
        // is unaffected — the gate is "at least", not "exactly".
        //
        // The X-Stainless-* values below are deliberately NOT touched: they
        // describe the SDK/runtime, not the CLI, and the backend pairs UA with
        // them as one registered client identity. Changing them speculatively
        // is how a working fingerprint gets broken.
        if (isOAuth) {
            builder.header("User-Agent", "claude-cli/2.1.280 (external, cli)")
            builder.header("X-Stainless-Lang", "js")
            builder.header("X-Stainless-Package-Version", "0.106.0")
            builder.header("X-Stainless-OS", "Linux")
            builder.header("X-Stainless-Arch", "arm64")
            builder.header("X-Stainless-Runtime", "node")
            builder.header("X-Stainless-Runtime-Version", "v24.18.0")
            builder.header("X-Stainless-Retry-Count", "0")
            builder.header("X-Stainless-Timeout", "600")
            builder.header("X-App", "cli")
            builder.header("Anthropic-Dangerous-Direct-Browser-Access", "true")
        }

        if (isOAuth) {
            builder.header("Authorization", "Bearer $apiKey")
        } else if (apiKey.isEmpty()) {
            // [T-empty-key-compat-endpoints] Keyless third-party
            // Anthropic-compatible endpoint: send NO auth header rather than
            // a malformed `Bearer ` / empty x-api-key that strict relays
            // reject. Only reachable for custom-endpoint instances — routing
            // never builds a keyless provider for the official API.
        } else if (isCustomEndpoint) {
            builder.header("Authorization", "Bearer $apiKey")
        } else {
            builder.header("x-api-key", apiKey)
        }

        // [T-provider-custom-user-agent] Applied last so a non-blank override
        // wins over the OAuth claude-cli UA above. null/blank → fall back to
        // the branded Minis UA on the regular apiKey path, but on the OAuth
        // path keep the claude-cli/2.1.280 fingerprint set above (the
        // Anthropic OAuth backend pairs UA + X-Stainless-* and rejects calls
        // whose UA doesn't match the registered client identity). T-android-
        // default-ua: pass defaultUserAgent=null on OAuth, branded default
        // everywhere else.
        builder.applyUserAgentOverride(
            customUserAgent,
            defaultUserAgent = if (isOAuth) null else com.openminis.app.provider.MinisUserAgent.DEFAULT,
        )
        return builder.build()
    }

    private fun parseResponse(json: JSONObject): LLMResponse {
        val content = json.optJSONArray("content")
        val text = buildString {
            if (content != null) {
                for (i in 0 until content.length()) {
                    val block = content.getJSONObject(i)
                    if (block.safeOptString("type", "") == "text") {
                        append(block.safeOptString("text", ""))
                    }
                }
            }
        }
        val stopReason = json.safeOptString("stop_reason", "").ifEmpty { null }
        val usage = json.optJSONObject("usage")?.let { parseUsage(it) }
        return LLMResponse(text, stopReason, usage)
    }

    /**
     * [T-anthropic-ctx-cache-undercount] Anthropic's `input_tokens` is the
     * UNCACHED remainder only — the prompt the model saw is
     * `input_tokens + cache_read_input_tokens + cache_creation_input_tokens`.
     * `latestContextTokens` used to be `input_tokens` alone, so the better the
     * cache hit rate, the smaller the context looked: on a 150k-token history
     * with a 99% hit it reported ~1.5k. Every capacity decision keyed on it
     * (offload threshold, compaction, dynamicMaxTokens, the persona reminder,
     * the context ring) fired late or never. OpenAI/Gemini already report the
     * full prompt here; this brings Anthropic onto the same scale.
     *
     * `message_delta` usage frames may repeat only `output_tokens`; returning
     * 0 there (all input fields absent) keeps the caller's
     * `latestContextTokens > 0` guard from overwriting the real value that
     * `message_start` delivered.
     */
    internal fun parseUsage(json: JSONObject): LLMUsage {
        val inputTokens = json.optInt("input_tokens", 0)
        val cacheCreation = json.optInt("cache_creation_input_tokens", 0)
        val cacheRead = json.optInt("cache_read_input_tokens", 0)
        return LLMUsage(
            inputTokens = inputTokens,
            outputTokens = json.optInt("output_tokens", 0),
            cacheCreationInputTokens = cacheCreation.takeIf { it > 0 },
            cacheReadInputTokens = cacheRead.takeIf { it > 0 },
            latestContextTokens = inputTokens + cacheCreation + cacheRead,
        )
    }

    /**
     * [T-anthropic-delta-usage-merge] Combine a `message_delta` usage frame with
     * the `message_start` one. Fields the delta actually carries win (they are
     * cumulative on the official API); absent/zero input-side fields fall back
     * to the start frame so a relay's output-only delta keeps the real counts.
     */
    internal fun mergeDeltaUsage(start: LLMUsage?, delta: LLMUsage): LLMUsage {
        if (start == null) return delta
        val input = if (delta.inputTokens > 0) delta.inputTokens else start.inputTokens
        val creation = delta.cacheCreationInputTokens ?: start.cacheCreationInputTokens
        val read = delta.cacheReadInputTokens ?: start.cacheReadInputTokens
        return LLMUsage(
            inputTokens = input,
            outputTokens = maxOf(delta.outputTokens, start.outputTokens),
            cacheCreationInputTokens = creation,
            cacheReadInputTokens = read,
            latestContextTokens = input + (creation ?: 0) + (read ?: 0),
        )
    }

    private fun mapHttpError(statusCode: Int, body: String): LLMError {
        val message = try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            val errorType = error?.safeOptString("type", "") ?: "error"
            val errorMessage = error?.safeOptString("message", "") ?: body
            "[$errorType] $errorMessage"
        } catch (_: Exception) {
            "HTTP $statusCode: ${body.take(500)}"
        }
        // Keep the server's explanation on auth errors: relays answer 401/403
        // for quota, disabled groups, model not allowed etc., and a bare
        // "Invalid API key" sent users hunting for a key problem that wasn't.
        if (statusCode == 401 || statusCode == 403) {
            return LLMError.InvalidApiKey(if (body.isBlank()) "" else message)
        }
        if (statusCode == 429) return LLMError.RateLimited()

        val transientCodes = setOf(500, 502, 503, 504, 529)
        if (statusCode in transientCodes) {
            // [T-android-503-fallback] See OpenAIProvider.mapHttpError — the
            // status must survive onto the error so fallback can act on it.
            return LLMError.TransientError(message, httpStatus = statusCode)
        }
        return LLMError.ProviderError(message, httpStatus = statusCode)
    }

    private fun mapError(error: Throwable): LLMError {
        if (error is LLMError) return error
        if (error is java.io.IOException) return LLMError.NetworkError(error)
        return LLMError.Unknown(error)
    }
}

package com.openminis.app.ui.chat

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [T-spectra-v4-thought] On-device English → Chinese translation for thinking
 * text (ML Kit). Nothing leaves the phone: the ~30 MB language pack is
 * downloaded once, after that translation is offline.
 *
 * Text is translated line by line so paragraphs, lists and blank lines keep
 * their shape; fenced code blocks and lines without Latin letters are passed
 * through verbatim. Results are cached per (block, content) in a small LRU so
 * scrolling a card away and back does not translate it again.
 */
internal object ThinkingTranslator {

    enum class Phase { Downloading, Translating }

    private const val CACHE_SIZE = 48
    private val cache = object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > CACHE_SIZE
    }
    private val mutex = Mutex()
    @Volatile private var translator: Translator? = null

    private fun key(id: String, text: String) = "$id:${text.length}:${text.hashCode()}"

    fun cached(id: String, text: String): String? = synchronized(cache) { cache[key(id, text)] }

    /**
     * Cheap language sniff over the first 3000 chars: mostly Latin letters and
     * almost no CJK. Good enough to decide whether to offer "Translate".
     */
    fun looksEnglish(text: String): Boolean {
        if (text.isBlank()) return false
        var latin = 0
        var cjk = 0
        val n = minOf(text.length, 3000)
        for (i in 0 until n) {
            val c = text[i]
            when {
                c in 'a'..'z' || c in 'A'..'Z' -> latin++
                c.code in 0x3040..0x30FF || c.code in 0x3400..0x9FFF || c.code in 0xAC00..0xD7AF -> cjk++
            }
        }
        return latin >= 24 && cjk * 6 < latin
    }

    private fun client(): Translator =
        translator ?: synchronized(this) {
            translator ?: Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.CHINESE)
                    .build(),
            ).also { translator = it }
        }

    suspend fun translate(
        @Suppress("UNUSED_PARAMETER") context: Context,
        id: String,
        text: String,
        onPhase: (Phase) -> Unit,
    ): Result<String> = runCatching {
        cached(id, text)?.let { return@runCatching it }
        mutex.withLock {
            cached(id, text)?.let { return@withLock it }
            val c = client()
            val ready = runCatching {
                RemoteModelManager.getInstance()
                    .isModelDownloaded(TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build())
                    .awaitTask()
            }.getOrDefault(false)
            if (!ready) {
                withContext(Dispatchers.Main) { onPhase(Phase.Downloading) }
                c.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
            }
            withContext(Dispatchers.Main) { onPhase(Phase.Translating) }
            val out = StringBuilder(text.length)
            var inFence = false
            text.split('\n').forEachIndexed { i, line ->
                if (i > 0) out.append('\n')
                val trimmed = line.trimStart()
                if (trimmed.startsWith("```")) { inFence = !inFence; out.append(line); return@forEachIndexed }
                if (inFence || line.isBlank() || line.none { it in 'a'..'z' || it in 'A'..'Z' }) {
                    out.append(line); return@forEachIndexed
                }
                val lead = line.substring(0, line.length - trimmed.length)
                out.append(lead).append(c.translate(trimmed).awaitTask())
            }
            val result = out.toString()
            synchronized(cache) { cache[key(id, text)] = result }
            result
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

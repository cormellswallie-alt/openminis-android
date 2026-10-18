package com.openminis.app.data

/**
 * [T-memory-dedupe] Drop daily-log lines that merely repeat GLOBAL.md.
 *
 * Both are injected into every system prompt. When the user asks for a rule
 * to be remembered, it often lands in the daily log (memory_write) AND, later,
 * in GLOBAL.md — so the model reads the same instruction twice, sometimes in
 * slightly different wording, on every request. GLOBAL.md wins; the daily copy
 * is removed from the injected text only (the file on disk is untouched).
 *
 * Matching is near-duplicate, not exact: lines are normalised to letters and
 * digits (so punctuation / list markers / spacing do not matter) and compared
 * by character-bigram Dice similarity, which also works for CJK text where
 * there are no word boundaries. A heading or `<!-- timestamp -->` marker whose
 * whole entry was removed is removed with it.
 *
 * When nothing is dropped the input is returned unchanged (same instance), so
 * the prompt stays byte-stable for prompt caching.
 */
object MemoryDedupe {

    const val DEFAULT_THRESHOLD = 0.8
    /** Lines shorter than this (after normalisation) are never treated as duplicates. */
    private const val MIN_LEN = 10

    fun normalize(line: String): String =
        line.lowercase().filter { it.isLetterOrDigit() }

    private fun bigrams(s: String): Set<String> {
        if (s.length < 2) return setOf(s)
        val out = HashSet<String>(s.length)
        for (i in 0 until s.length - 1) out.add(s.substring(i, i + 2))
        return out
    }

    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val ba = bigrams(a)
        val bb = bigrams(b)
        if (ba.isEmpty() || bb.isEmpty()) return 0.0
        val inter = ba.count { it in bb }
        return 2.0 * inter / (ba.size + bb.size)
    }

    private fun isMarker(line: String): Boolean = line.trimStart().startsWith("<!--")
    private fun isHeading(line: String): Boolean = line.trimStart().startsWith("#")

    fun filterAgainstGlobal(daily: String, global: String?, threshold: Double = DEFAULT_THRESHOLD): String {
        if (global.isNullOrBlank() || daily.isEmpty()) return daily
        val globalNorms = global.lines()
            .filterNot { isHeading(it) || isMarker(it) }
            .map(::normalize)
            .filter { it.length >= MIN_LEN }
        if (globalNorms.isEmpty()) return daily
        val globalSet = globalNorms.toHashSet()
        val globalGrams = globalNorms.map { it to bigrams(it) }

        fun isDuplicate(line: String): Boolean {
            if (isHeading(line) || isMarker(line)) return false
            val n = normalize(line)
            if (n.length < MIN_LEN) return false
            if (n in globalSet) return true
            val g = bigrams(n)
            return globalGrams.any { (gn, gg) ->
                // Cheap length gate first: very different lengths cannot reach the threshold.
                val ratio = minOf(n.length, gn.length).toDouble() / maxOf(n.length, gn.length)
                ratio >= threshold * 0.75 && run {
                    val inter = g.count { it in gg }
                    2.0 * inter / (g.size + gg.size) >= threshold
                }
            }
        }

        val lines = daily.lines()
        var dropped = 0
        val out = ArrayList<String>(lines.size)
        // Headings / markers / blanks seen since the last kept content line.
        // Emitted only once content under them survives.
        val pending = ArrayList<String>()
        for (line in lines) {
            when {
                isMarker(line) -> {
                    // A new entry begins: whatever was pending belonged to an entry that is now empty.
                    pending.clear(); pending.add(line)
                }
                isHeading(line) -> {
                    val keepMarkers = pending.filter { isMarker(it) }
                    pending.clear(); pending.addAll(keepMarkers); pending.add(line)
                }
                line.isBlank() -> if (pending.isNotEmpty()) pending.add(line) else out.add(line)
                isDuplicate(line) -> dropped++
                else -> {
                    out.addAll(pending); pending.clear()
                    out.add(line)
                }
            }
        }
        if (dropped == 0) return daily
        // Trailing pending headings had no surviving content; drop them.
        while (out.isNotEmpty() && out.last().isBlank()) out.removeAt(out.size - 1)
        return out.joinToString("\n")
    }
}

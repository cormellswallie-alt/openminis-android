package com.openminis.app.tools

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * [T-file-tool-race] Serialise file_write / file_edit on the same file.
 *
 * Tool calls of one assistant turn run CONCURRENTLY (MAX_CONCURRENT_TOOLS).
 * A model that emits several file_edit calls against one file in a single
 * turn — common, it is how a multi-hunk change is expressed — used to run
 * read → replace → writeText in parallel:
 *   - lost update: every edit read the ORIGINAL text, so only the last writer's
 *     single hunk survived;
 *   - tail corruption: writeText truncates then writes; when a longer write
 *     lands first and a shorter one second through a stale size, bytes of the
 *     longer version were left after the shorter content.
 * Observed on device: five edits to ExecutionCoordinator.kt in one turn left
 * two of them applied and ~60 bytes of a duplicate tail at end of file.
 *
 * Holding one lock per canonical path across the whole read-modify-write makes
 * each edit see the previous one's result. Different files stay parallel.
 * The executors are blocking (Dispatchers.IO), so a plain monitor is fine.
 */
object FileMutationLock {
    @PublishedApi internal val locks = ConcurrentHashMap<String, Any>()

    @PublishedApi internal fun keyFor(file: File): String =
        try { file.canonicalPath } catch (_: Exception) { file.absolutePath }

    /** Inline so callers may `return` from inside [block]. */
    inline fun <T> withLock(file: File, block: () -> T): T {
        val lock = locks.computeIfAbsent(keyFor(file)) { Any() }
        return synchronized(lock) { block() }
    }
}

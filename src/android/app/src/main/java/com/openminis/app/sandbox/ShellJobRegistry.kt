package com.openminis.app.sandbox

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * [T-shell-auto-background] shell_execute commands that outlive their
 * `yield_after` budget.
 *
 * Instead of blocking the agent turn for minutes, a long command is detached:
 * the tool call returns the output produced so far plus a job id, and the
 * command keeps running in [scope]. The model then polls it through the
 * `minis-shell-job` CLI (see ShellJobOffloadHandler) — `wait` blocks until the
 * job finishes or a short timeout passes, `status` returns at once, `kill`
 * stops it.
 *
 * Output is collected line by line from the executor's line callback into a
 * bounded buffer. Every read returns only what was not returned before (a
 * cursor per job), so repeated polling never re-sends old output to the model.
 *
 * Jobs are in-memory only: an app process death ends them (and their proot
 * process with it). The user's Stop button and session deletion still reach a
 * detached job, because it runs through the same ExecutionCoordinator registry
 * as any foreground command.
 */
object ShellJobRegistry {

    /** Default seconds a shell_execute call blocks before the command is detached. */
    const val DEFAULT_YIELD_SEC = 60
    const val MIN_YIELD_SEC = 5
    const val MAX_YIELD_SEC = 240

    /** `minis-shell-job wait` bounds. */
    const val DEFAULT_WAIT_SEC = 120
    const val MAX_WAIT_SEC = 240

    /** Name of the guest CLI that polls jobs. */
    const val CLI_NAME = "minis-shell-job"

    private const val MAX_BUFFER_CHARS = 2_000_000
    internal const val MAX_READ_CHARS = 30_000
    private const val MAX_FINISHED_JOBS = 50

    /** Detached jobs outlive the tool call (and the ViewModel), so they need a scope of their own. */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    enum class State(val label: String) {
        RUNNING("running"),
        DONE("finished"),
        KILLED("killed"),
        TIMED_OUT("timed out"),
    }

    class Job internal constructor(
        val id: String,
        val sessionId: String,
        val command: String,
        val timeoutSec: Int,
    ) {
        val startedAt: Long = System.currentTimeMillis()
        private val lock = Any()
        private val buffer = StringBuilder()
        /** Chars dropped from the head of [buffer] to keep it bounded. */
        private var droppedChars = 0L
        /** Absolute offset (in the never-trimmed stream) already returned to the model. */
        private var readCursor = 0L

        @Volatile var finishedAt: Long? = null
            private set
        @Volatile var exitCode: Int? = null
            private set
        @Volatile var state: State = State.RUNNING
            private set
        /** True once the tool call has returned and the job runs on its own. */
        @Volatile var detached: Boolean = false
            internal set
        @Volatile private var killRequested = false
        @Volatile private var stopper: (() -> Unit)? = null

        internal val done = CompletableDeferred<Unit>()

        fun appendLine(line: String) {
            synchronized(lock) {
                buffer.append(line).append('\n')
                val excess = buffer.length - MAX_BUFFER_CHARS
                if (excess > 0) {
                    buffer.delete(0, excess)
                    droppedChars += excess
                }
            }
        }

        /** Called by the executor whenever a process for this job starts (a self-heal retry starts a second one). */
        fun setStopper(stop: () -> Unit) {
            stopper = stop
            if (killRequested) runCatching { stop() }
        }

        fun kill(): Boolean {
            if (state != State.RUNNING) return false
            killRequested = true
            stopper?.let { runCatching { it() } }
            return true
        }

        internal fun finish(code: Int) {
            if (finishedAt != null) return
            exitCode = code
            state = when {
                killRequested || code == 130 -> State.KILLED
                code == 124 -> State.TIMED_OUT
                else -> State.DONE
            }
            finishedAt = System.currentTimeMillis()
            done.complete(Unit)
        }

        /**
         * Output not yet returned, capped at [maxChars] (the newest part is
         * kept). Advances the cursor. Returns (text, skippedChars).
         */
        fun readNew(maxChars: Int = MAX_READ_CHARS): Pair<String, Long> = synchronized(lock) {
            val totalWritten = droppedChars + buffer.length
            var start = readCursor
            var skipped = 0L
            if (start < droppedChars) {
                skipped += droppedChars - start
                start = droppedChars
            }
            val available = totalWritten - start
            if (available > maxChars) {
                skipped += available - maxChars
                start = totalWritten - maxChars
            }
            val text = buffer.substring((start - droppedChars).toInt())
            readCursor = totalWritten
            text to skipped
        }

        fun elapsedSec(): Long = ((finishedAt ?: System.currentTimeMillis()) - startedAt) / 1000
    }

    private val jobs = ConcurrentHashMap<String, Job>()

    fun create(sessionId: String, command: String, timeoutSec: Int): Job {
        var id: String
        do {
            id = "sh_" + java.util.UUID.randomUUID().toString().replace("-", "").take(6)
        } while (jobs.containsKey(id))
        val job = Job(id, sessionId, command, timeoutSec)
        jobs[id] = job
        return job
    }

    /** The command finished inside its yield budget: it never became a job the model knows about. */
    fun discard(job: Job) {
        if (!job.detached) jobs.remove(job.id)
    }

    fun markDetached(job: Job) {
        job.detached = true
        prune()
    }

    /**
     * Record the exit. The entry is NOT removed here even when the job never
     * detached: the tool call may be deciding to detach at this very moment,
     * and a job it then announces must still be findable. [discard] removes
     * inline-finished jobs once the tool call has its result.
     */
    fun finish(job: Job, exitCode: Int) {
        job.finish(exitCode)
    }

    /** Exact id, or a unique prefix of one. */
    fun find(idOrPrefix: String): Job? {
        val key = idOrPrefix.trim()
        if (key.isEmpty()) return null
        jobs[key]?.let { return it.takeIf { j -> j.detached } }
        val matches = jobs.values.filter { it.detached && it.id.startsWith(key) }
        return matches.singleOrNull()
    }

    fun list(sessionId: String?): List<Job> =
        jobs.values
            .filter { it.detached && (sessionId == null || it.sessionId == sessionId) }
            .sortedBy { it.startedAt }

    /** Block (on a worker thread) until [job] finishes or [timeoutSec] passes. */
    fun awaitBlocking(job: Job, timeoutSec: Int): Boolean = runBlocking {
        withTimeoutOrNull(timeoutSec * 1000L) { job.done.await() } != null
    }

    private fun prune() {
        val finished = jobs.values.filter { it.finishedAt != null }.sortedBy { it.finishedAt }
        val excess = finished.size - MAX_FINISHED_JOBS
        if (excess > 0) finished.take(excess).forEach { jobs.remove(it.id) }
    }

    /** Human/model-readable report: state line, new output, and next-step hint. */
    fun report(job: Job, maxChars: Int = MAX_READ_CHARS): String {
        val (text, skipped) = job.readNew(maxChars)
        return buildString {
            append("job ").append(job.id).append(": ").append(job.state.label)
            job.exitCode?.let { append(" (exit code ").append(it).append(")") }
            append(", elapsed ").append(job.elapsedSec()).append("s")
            if (job.state == State.RUNNING) append(" of ").append(job.timeoutSec).append("s timeout")
            append('\n')
            if (skipped > 0) append("[... ").append(skipped).append(" earlier chars omitted ...]\n")
            if (text.isEmpty()) append("(no new output)\n") else append(text)
            if (job.state == State.RUNNING) {
                if (!text.endsWith("\n") && text.isNotEmpty()) append('\n')
                append("Still running. Check again with `").append(CLI_NAME).append(" wait ")
                    .append(job.id).append("`, or stop it with `").append(CLI_NAME).append(" kill ")
                    .append(job.id).append("`.")
            }
        }.trimEnd()
    }
}

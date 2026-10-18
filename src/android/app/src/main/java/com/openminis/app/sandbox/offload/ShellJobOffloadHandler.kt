package com.openminis.app.sandbox.offload

import com.openminis.app.data.EnvVarRedactor
import com.openminis.app.sandbox.NativeOffloadHandler
import com.openminis.app.sandbox.NativeOffloadRequest
import com.openminis.app.sandbox.NativeOffloadResult
import com.openminis.app.sandbox.ShellJobRegistry

/**
 * [T-shell-auto-background] `minis-shell-job` — poll / stop shell_execute
 * commands that were moved to the background after their `yield_after`
 * budget (see [ShellJobRegistry]).
 *
 *   minis-shell-job list
 *   minis-shell-job status <id>              # new output + state, returns at once
 *   minis-shell-job wait <id> [--timeout S]  # block until finished or S seconds (default 120, max 240)
 *   minis-shell-job kill <id>
 *
 * Exit codes: `status`/`wait` mirror the job — the job's own exit code once it
 * has finished, 0 while it is still running. Unknown job → 2, usage error → 64.
 * shell_execute never backgrounds a command that starts with this CLI, so a
 * long `wait` cannot turn into a job of its own.
 */
class ShellJobOffloadHandler : NativeOffloadHandler {

    override fun handle(request: NativeOffloadRequest): NativeOffloadResult {
        val args = request.argv.drop(1)
        val sub = args.firstOrNull()?.lowercase()
        if (sub == null || sub == "help" || sub == "--help" || sub == "-h") {
            return NativeOffloadResult(if (sub == null) 64 else 0, HELP)
        }
        if (sub == "list" || sub == "ls") return list(request.sessionId, args.drop(1))

        val id = args.getOrNull(1)?.takeIf { !it.startsWith("--") }
            ?: return NativeOffloadResult(64, "$CLI: '$sub' needs a job id. Run `$CLI list` to see jobs.\n")
        val job = ShellJobRegistry.find(id)
            ?: return NativeOffloadResult(
                2,
                "$CLI: no background job '$id' (finished jobs are kept for a while; the list is lost when the app restarts). " +
                    "Run `$CLI list`.\n",
            )

        return when (sub) {
            "status", "output", "logs" -> reportResult(job)
            "wait" -> {
                val timeout = parseTimeout(args.drop(2))
                    ?: return NativeOffloadResult(64, "$CLI: --timeout must be a number of seconds\n")
                if (job.state == ShellJobRegistry.State.RUNNING) ShellJobRegistry.awaitBlocking(job, timeout)
                reportResult(job)
            }
            "kill", "stop", "cancel" -> {
                if (job.kill()) {
                    ShellJobRegistry.awaitBlocking(job, 5)
                    reportResult(job, prefix = "Kill requested.\n")
                } else {
                    reportResult(job, prefix = "Job is not running any more.\n")
                }
            }
            else -> NativeOffloadResult(64, "$CLI: unknown subcommand '$sub'\n\n$HELP")
        }
    }

    private fun reportResult(job: ShellJobRegistry.Job, prefix: String = ""): NativeOffloadResult {
        val (redacted, _) = EnvVarRedactor.redactIfEnabled(ShellJobRegistry.report(job))
        val code = if (job.state == ShellJobRegistry.State.RUNNING) 0 else (job.exitCode ?: 0)
        return NativeOffloadResult(code, prefix + redacted + "\n")
    }

    private fun list(sessionId: String?, rest: List<String>): NativeOffloadResult {
        val all = rest.contains("--all")
        val jobs = ShellJobRegistry.list(if (all) null else sessionId)
        if (jobs.isEmpty()) return NativeOffloadResult(0, "No background jobs${if (all) "" else " in this chat"}.\n")
        val text = buildString {
            for (j in jobs) {
                append(j.id).append("  ").append(j.state.label)
                j.exitCode?.let { append(" (exit ").append(it).append(")") }
                append("  ").append(j.elapsedSec()).append("s  ")
                append(j.command.replace('\n', ' ').take(100)).append('\n')
            }
        }
        return NativeOffloadResult(0, text)
    }

    private fun parseTimeout(rest: List<String>): Int? {
        var value = ShellJobRegistry.DEFAULT_WAIT_SEC
        var i = 0
        while (i < rest.size) {
            val a = rest[i]
            val raw = when {
                a == "--timeout" || a == "-t" -> rest.getOrNull(++i)
                a.startsWith("--timeout=") -> a.substringAfter('=')
                else -> null
            }
            if (raw != null) value = raw.trim().removeSuffix("s").toIntOrNull() ?: return null
            i++
        }
        return value.coerceIn(1, ShellJobRegistry.MAX_WAIT_SEC)
    }

    companion object {
        private const val CLI = ShellJobRegistry.CLI_NAME
        private val HELP = """$CLI - poll or stop shell_execute commands that were moved to the background

Usage:
  $CLI list [--all]              background jobs of this chat (--all: every chat)
  $CLI status <id>               state + output produced since the last read; returns at once
  $CLI wait <id> [--timeout S]   block until the job finishes or S seconds pass (default ${ShellJobRegistry.DEFAULT_WAIT_SEC}, max ${ShellJobRegistry.MAX_WAIT_SEC}), then report
  $CLI kill <id>                 stop the job

A shell_execute call that is still running after its yield_after budget returns
its output so far and a job id (sh_xxxxxx); the command keeps running. Each
report only contains output not shown before. Exit code: the job's own once it
has finished, 0 while it is still running.
"""
    }
}

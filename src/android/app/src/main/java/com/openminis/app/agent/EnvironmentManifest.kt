package com.openminis.app.agent

import java.io.File

/**
 * [T-env-manifest] A few lines in the system prompt's Runtime context that
 * tell the model what the sandbox actually has, so it does not discover it by
 * failing.
 *
 * Measured motivation: a "read this branch from GitHub" task took ~10 tool
 * calls where 2 would do — `rg: not found`, `curl: not found`,
 * `python3: not found`, then probing for which credentials exist before
 * finding that GITHUB_TOKEN had been set all along.
 *
 * Only NAMES are listed. Env var values never enter the prompt.
 *
 * The snapshot is taken when the system prompt is (re)built — normally once
 * per conversation, because the prompt is frozen for prompt-cache stability —
 * so it is labelled as a snapshot; anything installed later is simply not
 * reflected until the next rebuild. It sits in the Runtime context suffix, the
 * last part of the prompt, so a changed manifest invalidates nothing before it.
 */
object EnvironmentManifest {

    /** Tools the prompt or common tasks rely on, in display order. */
    val PROBED_TOOLS: List<String> = listOf(
        "rg", "curl", "wget", "git", "jq", "python3", "pip3", "node", "npm", "bash", "gcc", "make", "sqlite3", "ssh", "gh",
    )

    private val BIN_DIRS = listOf("usr/local/bin", "usr/bin", "bin", "usr/sbin", "sbin")

    private const val MAX_ENV_NAMES = 40

    /**
     * Names present in the rootfs bin directories. Directory listings include
     * symlinks (busybox applets) without resolving them, which matters: their
     * absolute targets would not resolve on the host side.
     */
    fun installedTools(rootfsDir: File?, probe: List<String> = PROBED_TOOLS): Set<String> {
        if (rootfsDir == null || !rootfsDir.isDirectory) return emptySet()
        val present = HashSet<String>()
        for (d in BIN_DIRS) {
            File(rootfsDir, d).list()?.let { present.addAll(it) }
        }
        return probe.filter { it in present }.toSet()
    }

    /** The Runtime-context lines (no trailing newline), or null when there is nothing to say. */
    fun render(
        installed: Set<String>?,
        envVarNames: Collection<String>,
        probe: List<String> = PROBED_TOOLS,
    ): String? {
        val lines = mutableListOf<String>()
        if (installed != null) {
            val have = probe.filter { it in installed }
            val missing = probe.filter { it !in installed }
            val sb = StringBuilder("- Sandbox tools (snapshot at conversation start): ")
            sb.append(if (have.isEmpty()) "none of the common ones" else "installed " + have.joinToString(" "))
            if (missing.isNotEmpty()) {
                sb.append("; NOT installed ").append(missing.joinToString(" "))
                sb.append(" (install on first use: apk add ripgrep curl git jq python3 …)")
            }
            lines += sb.toString()
        }
        val names = envVarNames.filter { it.isNotBlank() }.distinct().sorted()
        if (names.isNotEmpty()) {
            val shown = names.take(MAX_ENV_NAMES)
            val more = if (names.size > shown.size) " (+${names.size - shown.size} more)" else ""
            lines += "- Env vars set (names only — reference as \$NAME, never print values): " +
                shown.joinToString(" ") + more
        } else {
            lines += "- Env vars set: none"
        }
        return if (lines.isEmpty()) null else lines.joinToString("\n")
    }
}

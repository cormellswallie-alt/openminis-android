package com.openminis.app.agent.shell

/**
 * [T-shell-missing-command-hint] Turn "command not found" into the install
 * step the model would otherwise have to work out itself.
 *
 * The default rootfs is a bare Alpine: rg, curl, python3, git, jq are all
 * absent, and the system prompt tells the model to use several of them. Every
 * first contact used to cost a round trip — fail, reason about it, `apk add`,
 * retry. Appending the exact package name to the failed result lets the model
 * fix it in the very next call (and usually in the same command line).
 *
 * Pure function, no Android dependencies, so it is unit-tested directly.
 */
object MissingCommandHint {

    /** Command name → Alpine package that provides it. */
    private val PACKAGES: Map<String, String> = mapOf(
        "rg" to "ripgrep",
        "fd" to "fd",
        "curl" to "curl",
        "git" to "git",
        "jq" to "jq",
        "yq" to "yq",
        "python3" to "python3",
        "python" to "python3",
        "pip" to "py3-pip",
        "pip3" to "py3-pip",
        "node" to "nodejs",
        "npm" to "npm",
        "npx" to "npm",
        "bash" to "bash",
        "make" to "make",
        "gcc" to "build-base",
        "g++" to "build-base",
        "cc" to "build-base",
        "file" to "file",
        "sqlite3" to "sqlite",
        "ssh" to "openssh-client",
        "scp" to "openssh-client",
        "rsync" to "rsync",
        "zip" to "zip",
        "7z" to "7zip",
        "ffmpeg" to "ffmpeg",
        "ffprobe" to "ffmpeg",
        "convert" to "imagemagick",
        "magick" to "imagemagick",
        "gh" to "github-cli",
        "go" to "go",
        "cargo" to "cargo",
        "rustc" to "rust",
        "perl" to "perl",
        "ruby" to "ruby",
        "tree" to "tree",
        "htop" to "htop",
        "vim" to "vim",
        "nano" to "nano",
        "less" to "less",
        "pandoc" to "pandoc",
        "openssl" to "openssl",
        "psql" to "postgresql-client",
        "mysql" to "mariadb-client",
        "redis-cli" to "redis",
        "dig" to "bind-tools",
        "nslookup" to "bind-tools",
        "xmllint" to "libxml2-utils",
        "tesseract" to "tesseract-ocr",
        "pdftotext" to "poppler-utils",
    )

    /**
     * Matches the shells' "not found" diagnostics:
     *   /bin/sh: rg: not found          (busybox ash)
     *   sh: 1: rg: not found            (dash)
     *   bash: rg: command not found     (bash)
     *   bash: line 3: rg: command not found
     */
    private val NOT_FOUND = Regex(
        """(?m)(?:^|\s)(?:/bin/|/usr/bin/)?(?:ba|a)?sh(?:: line \d+)?(?:: \d+)?: ([A-Za-z0-9._+\-]+): (?:command )?not found""",
    )

    private const val MAX_NAMES = 5

    /** Names of commands the output reports as missing, in first-seen order. */
    fun missingCommands(output: String): List<String> {
        if (!output.contains("not found")) return emptyList()
        return NOT_FOUND.findAll(output)
            .map { it.groupValues[1] }
            .filter { it.isNotEmpty() && !it.startsWith("-") }
            .distinct()
            .take(MAX_NAMES)
            .toList()
    }

    /** A `<system-reminder>` to append to the tool result, or null when nothing is missing. */
    fun hintFor(output: String): String? {
        val missing = missingCommands(output)
        if (missing.isEmpty()) return null
        val known = missing.filter { PACKAGES.containsKey(it) }
        val unknown = missing - known.toSet()
        return buildString {
            append("<system-reminder>Not installed in this Alpine sandbox: ")
            append(missing.joinToString(", ") { "`$it`" }).append(". ")
            if (known.isNotEmpty()) {
                val pkgs = known.map { PACKAGES.getValue(it) }.distinct()
                append("Install once with `apk add ").append(pkgs.joinToString(" ")).append("`")
                append(" (it persists across calls), e.g. prefix your command with `apk add -q ")
                    .append(pkgs.joinToString(" ")).append(" >/dev/null 2>&1 && `. ")
            }
            if (unknown.isNotEmpty()) {
                append("For ").append(unknown.joinToString(", ") { "`$it`" })
                append(" find the package with `apk search -q <name>`, or check for a typo. ")
            }
            append("</system-reminder>")
        }
    }
}

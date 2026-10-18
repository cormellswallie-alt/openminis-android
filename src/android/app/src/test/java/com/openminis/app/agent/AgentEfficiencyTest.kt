package com.openminis.app.agent

import com.openminis.app.agent.shell.MissingCommandHint
import com.openminis.app.data.MemoryDedupe
import com.openminis.app.sandbox.ShellJobRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * [T-shell-auto-background] [T-shell-missing-command-hint] [T-env-manifest]
 * [T-memory-dedupe] Pure-logic halves of the agent-efficiency changes.
 */
class AgentEfficiencyTest {

    // ---- MissingCommandHint ----

    @Test
    fun `busybox not-found names the apk package`() {
        val hint = MissingCommandHint.hintFor("/bin/sh: rg: not found\n/bin/sh: python3: not found")
        assertNotNull(hint)
        assertTrue(hint!!, hint.contains("apk add ripgrep python3"))
        assertTrue(hint.contains("`rg`"))
    }

    @Test
    fun `bash and dash variants are recognised`() {
        assertEquals(listOf("jq"), MissingCommandHint.missingCommands("bash: line 3: jq: command not found"))
        assertEquals(listOf("curl"), MissingCommandHint.missingCommands("sh: 1: curl: not found"))
    }

    @Test
    fun `unknown command gets apk search advice, ordinary output gets nothing`() {
        val hint = MissingCommandHint.hintFor("/bin/sh: frobnicate: not found")!!
        assertTrue(hint.contains("apk search"))
        assertFalse(hint.contains("apk add"))
        assertNull(MissingCommandHint.hintFor("file not found: foo.txt"))
        assertNull(MissingCommandHint.hintFor("all good"))
    }

    // ---- EnvironmentManifest ----

    @Test
    fun `manifest lists installed, missing and env var names only`() {
        val root = Files.createTempDirectory("rootfs").toFile()
        try {
            File(root, "usr/bin").mkdirs()
            File(root, "usr/bin/git").writeText("")
            File(root, "bin").mkdirs()
            File(root, "bin/jq").writeText("")
            val installed = EnvironmentManifest.installedTools(root, listOf("git", "jq", "rg"))
            assertEquals(setOf("git", "jq"), installed)
            val text = EnvironmentManifest.render(installed, listOf("GITHUB_TOKEN", "API_KEY"), listOf("git", "jq", "rg"))!!
            assertTrue(text, text.contains("installed git jq"))
            assertTrue(text, text.contains("NOT installed rg"))
            assertTrue(text, text.contains("API_KEY GITHUB_TOKEN"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `manifest without rootfs still reports env names`() {
        val text = EnvironmentManifest.render(null, emptyList())!!
        assertFalse(text.contains("Sandbox tools"))
        assertTrue(text.contains("Env vars set: none"))
    }

    // ---- MemoryDedupe ----

    private val global = """# GLOBAL

## 工具调用规则
- 调用工具时必须使用当前会话提供的准确工具名，保持不变；禁止使用猜测或凭记忆的其他名字（否则会报错）。
- 单次工具调用最长不得超过 4 分钟：shell 命令 timeout ≤ 240 秒，delay + 执行总时长也不超过 4 分钟；更长的任务需拆分成多次调用，或后台运行后分次轮询。
"""

    @Test
    fun `daily entry that only repeats GLOBAL is removed with its heading`() {
        val daily = """<!-- 2026-10-17 23:21:59 -->
## 工具调用规则（用户要求）
- 调用工具时必须使用当前会话提供的准确工具名，保持不变，不得用猜测或凭记忆的其他名字（否则会报错）。
- 单次工具调用最长不得超过 4 分钟：shell 类命令 timeout ≤ 240 秒，delay + 执行总时长也不超过 4 分钟；更长的任务需拆分成多次调用或后台运行后轮询。

<!-- 2026-10-17 23:40:00 -->
## Project
- openminis-android lives in /var/minis/workspace/om
"""
        val out = MemoryDedupe.filterAgainstGlobal(daily, global)
        assertFalse(out, out.contains("工具调用规则"))
        assertFalse(out, out.contains("23:21:59"))
        assertTrue(out, out.contains("## Project"))
        assertTrue(out, out.contains("openminis-android lives"))
    }

    @Test
    fun `nothing to drop returns the same instance for cache stability`() {
        val daily = "## Notes\n- user prefers Kotlin over Java for new modules\n"
        assertSame(daily, MemoryDedupe.filterAgainstGlobal(daily, global))
        assertSame(daily, MemoryDedupe.filterAgainstGlobal(daily, null))
    }

    // ---- ShellJobRegistry ----

    @Test
    fun `job reads only new output and reports exit`() {
        val job = ShellJobRegistry.create("s1", "make", 600)
        ShellJobRegistry.markDetached(job)
        job.appendLine("a")
        job.appendLine("b")
        assertEquals("a\nb\n", job.readNew().first)
        assertEquals("", job.readNew().first)
        job.appendLine("c")
        ShellJobRegistry.finish(job, 0)
        val report = ShellJobRegistry.report(job)
        assertTrue(report, report.contains("finished (exit code 0)"))
        assertTrue(report, report.contains("c"))
        assertFalse(report, report.contains("Still running"))
        assertSame(job, ShellJobRegistry.find(job.id.take(6)))
    }

    @Test
    fun `read cap keeps the newest output and counts skipped chars`() {
        val job = ShellJobRegistry.create("s1", "yes", 600)
        repeat(100) { job.appendLine("line-$it") }
        val (text, skipped) = job.readNew(maxChars = 20)
        assertEquals(20, text.length)
        assertTrue(text.endsWith("line-99\n"))
        assertTrue(skipped > 0)
        ShellJobRegistry.discard(job)
        assertNull(ShellJobRegistry.find(job.id))
    }

    @Test
    fun `kill reaches the stopper even when it is installed later`() {
        val job = ShellJobRegistry.create("s1", "sleep 999", 600)
        assertTrue(job.kill())
        var stopped = false
        job.setStopper { stopped = true }
        assertTrue(stopped)
        ShellJobRegistry.finish(job, 130)
        assertEquals(ShellJobRegistry.State.KILLED, job.state)
        assertFalse(job.kill())
        ShellJobRegistry.discard(job)
    }

    @Test
    fun `awaitBlocking returns once finished`() {
        val job = ShellJobRegistry.create("s1", "true", 60)
        ShellJobRegistry.finish(job, 0)
        assertTrue(ShellJobRegistry.awaitBlocking(job, 1))
        ShellJobRegistry.discard(job)
    }
}

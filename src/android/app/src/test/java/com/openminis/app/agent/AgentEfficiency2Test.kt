package com.openminis.app.agent

import com.openminis.app.data.ContextOffload
import com.openminis.app.tools.FileMutationLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AgentEfficiency2Test {

    // ── [T-offload-stub-preview] ────────────────────────────────────────

    private val path = "${ContextOffload.LINUX_OFFLOADS_DIR}/tools/shell_execute_x.txt"

    @Test
    fun `preview keeps head and tail and is still a placeholder`() {
        val content = (1..400).joinToString("\n") { "line $it of the build log" } + "\nBUILD SUCCESSFUL"
        val s = ContextOffload.stubWithPreview(3000, content.length, path, content)
        assertTrue(s.startsWith(ContextOffload.OFFLOADED_PREFIX))
        assertTrue(ContextOffload.isOffloadPlaceholder(s))
        assertTrue(s.contains(path))
        assertTrue(s.contains("line 1 of the build log"))
        assertTrue(s.contains("BUILD SUCCESSFUL"))
        assertFalse(s.contains("line 200 of the build log"))
        assertTrue(s.length < content.length / 5)
    }

    @Test
    fun `preview cuts on line boundaries`() {
        val content = (1..400).joinToString("\n") { "line $it of the build log" }
        val s = ContextOffload.stubWithPreview(3000, content.length, path, content)
        val preview = s.substringAfter("Preview (head … tail):\n")
        val head = preview.substringBefore("\n[…]\n")
        val tail = preview.substringAfter("\n[…]\n")
        assertTrue(head.lines().last().matches(Regex("line \\d+ of the build log")))
        assertTrue(tail.lines().first().matches(Regex("line \\d+ of the build log")))
    }

    @Test
    fun `short content gets the plain stub`() {
        val content = "x".repeat(900)
        assertEquals(
            ContextOffload.stub(10, 900, path),
            ContextOffload.stubWithPreview(10, 900, path, content),
        )
    }

    // ── [T-file-tool-race] ───────────────────────────────────────────────

    @Test
    fun `concurrent read-modify-write on one file loses no update`() {
        val f = File.createTempFile("race", ".txt").apply { writeText("") ; deleteOnExit() }
        val n = 40
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        repeat(n) { i ->
            pool.execute {
                start.await()
                FileMutationLock.withLock(f) {
                    val cur = f.readText()
                    Thread.sleep(1)
                    f.writeText(cur + "edit$i\n")
                }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
        val lines = f.readText().lines().filter { it.isNotEmpty() }
        assertEquals(n, lines.size)
        assertEquals(n, lines.toSet().size)
    }

    @Test
    fun `same file through different paths shares one lock`() {
        val dir = File(System.getProperty("java.io.tmpdir"))
        val f = File.createTempFile("alias", ".txt", dir).apply { deleteOnExit() }
        val alias = File(File(dir, "."), f.name)
        assertEquals(FileMutationLock.keyFor(f), FileMutationLock.keyFor(alias))
        assertNotEquals(FileMutationLock.keyFor(f), FileMutationLock.keyFor(File(dir, f.name + "_other")))
    }

    // ── [T-shell-auto-background] poll detection ────────────────────────

    private fun shell(cmd: String) = mapOf<String, Any?>("command" to cmd, "tool_title" to "poll")

    @Test
    fun `polling a job that makes progress never warns`() {
        val d = ToolLoopDetector()
        val p = shell("minis-shell-job wait sh_abc123")
        repeat(15) { i ->
            assertEquals(Level.NONE, d.check("shell_execute", p).level)
            d.record("shell_execute", p, "[job sh_abc123 running 0:${i}0] compiling module $i")
        }
    }

    @Test
    fun `identical ordinary commands still warn`() {
        val d = ToolLoopDetector()
        val p = shell("ls /tmp")
        var warned = false
        repeat(12) { i ->
            if (d.check("shell_execute", p).level == Level.WARNING) warned = true
            d.record("shell_execute", p, "out $i")
        }
        assertTrue(warned)
    }
}

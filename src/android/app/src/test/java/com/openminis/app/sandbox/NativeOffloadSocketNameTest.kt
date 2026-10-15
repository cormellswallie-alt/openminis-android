package com.openminis.app.sandbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [coexist-socket-fix] The abstract socket namespace is device-wide, so two
 * installed builds with different applicationIds must never share a name.
 */
class NativeOffloadSocketNameTest {
    @Test
    fun `upstream package keeps the original name`() {
        assertEquals("native-offload", NativeOffloadServer.socketNameFor("com.openminis.app"))
        assertEquals("native-offload", NativeOffloadServer.socketNameFor(null))
    }

    @Test
    fun `other packages get a distinct name`() {
        val fork = NativeOffloadServer.socketNameFor("com.openminis.plus")
        assertEquals("native-offload.com.openminis.plus", fork)
        assertNotEquals(NativeOffloadServer.socketNameFor("com.openminis.app"), fork)
    }

    @Test
    fun `name fits in sun_path`() {
        assertTrue(NativeOffloadServer.socketNameFor("a".repeat(300)).length <= 100)
    }
}

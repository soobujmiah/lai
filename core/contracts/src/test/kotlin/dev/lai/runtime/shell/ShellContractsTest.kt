package dev.lai.runtime.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ShellContractsTest {
    @Test
    fun `structured shell values preserve bounded execution state`() {
        val command = PrivilegedCommand("settings.get", mapOf("key" to "screen_brightness"))
        assertEquals("settings.get", command.operation)
        val result = ShellResult(0, "100", "", timedOut = false)
        assertEquals(0, result.exitCode)
        assertFalse(result.timedOut)
        assertEquals(2000, (ShizukuState.Ready(2000)).uid)
        assertEquals("x", ShizukuState.Error("x").message)
    }
}

package dev.lai.runtime.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandPolicyTest {
    @Test
    fun `force stop accepts package and requires confirmation`() {
        val command = ShellCommandPolicy.compile(
            PrivilegedCommand("package.force_stop", mapOf("package" to "com.example.app")),
        ).getOrThrow()
        assertEquals(listOf("am", "force-stop", "com.example.app"), command.argv)
        assertTrue(command.requiresConfirmation)
    }

    @Test
    fun `package shell injection is rejected`() {
        assertTrue(
            ShellCommandPolicy.compile(
                PrivilegedCommand("package.force_stop", mapOf("package" to "com.safe.app;id")),
            ).isFailure,
        )
    }

    @Test
    fun `arbitrary operation is rejected`() {
        assertTrue(ShellCommandPolicy.compile(PrivilegedCommand("raw", mapOf("command" to "id"))).isFailure)
    }

    @Test
    fun `writing unknown setting is rejected`() {
        val result = ShellCommandPolicy.compile(
            PrivilegedCommand(
                "settings.put",
                mapOf("namespace" to "secure", "key" to "enabled_accessibility_services", "value" to "x"),
            ),
        )
        assertTrue(result.isFailure)
    }
}

package dev.lai.runtime.agent

import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPolicyTest {
    private val policy = AgentPolicy()

    @Test
    fun `screen observation requires accessibility`() {
        val decision = policy.review(
            ToolDefinition("screen.snapshot", "snapshot", ToolRisk.READ_ONLY, false),
            AuthorityContext(accessibilityConnected = false, elevatedServiceReady = false),
            userConfirmed = false,
        )
        assertTrue(decision is PolicyDecision.Deny)
    }

    @Test
    fun `elevated action requires service and confirmation`() {
        val tool = ToolDefinition("shell.operation", "shell", ToolRisk.ELEVATED, true)
        assertTrue(
            policy.review(tool, AuthorityContext(true, false), userConfirmed = true) is PolicyDecision.Deny,
        )
        assertTrue(
            policy.review(tool, AuthorityContext(true, true), userConfirmed = false) is PolicyDecision.Deny,
        )
        assertTrue(
            policy.review(tool, AuthorityContext(true, true), userConfirmed = true) is PolicyDecision.Allow,
        )
    }
}

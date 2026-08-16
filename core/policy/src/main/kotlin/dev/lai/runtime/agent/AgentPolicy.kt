package dev.lai.runtime.agent

enum class AuthorityLevel { OBSERVE, INTERACT, SENSITIVE, ELEVATED }
enum class ConsentScope { NONE, PER_ACTION }

data class AuthorityContext(
    val accessibilityConnected: Boolean,
    val elevatedServiceReady: Boolean,
)

sealed interface PolicyDecision {
    data object Allow : PolicyDecision
    data class Deny(val code: String, val reason: String) : PolicyDecision
}

class AgentPolicy {
    fun review(
        tool: ToolDefinition,
        authority: AuthorityContext,
        userConfirmed: Boolean,
    ): PolicyDecision {
        val requiredAuthority = when (tool.risk) {
            ToolRisk.READ_ONLY -> AuthorityLevel.OBSERVE
            ToolRisk.INTERACTION -> AuthorityLevel.INTERACT
            ToolRisk.SENSITIVE -> AuthorityLevel.SENSITIVE
            ToolRisk.ELEVATED -> AuthorityLevel.ELEVATED
        }
        val requiresScreenAuthority = tool.name.startsWith("screen.") || tool.name == "ocr.current_screen" ||
            (requiredAuthority >= AuthorityLevel.INTERACT && tool.name != "shell.operation")
        if (requiresScreenAuthority && !authority.accessibilityConnected) {
            return PolicyDecision.Deny("ACCESSIBILITY_REQUIRED", "Accessibility authority is not connected")
        }
        if (requiredAuthority == AuthorityLevel.ELEVATED && !authority.elevatedServiceReady) {
            return PolicyDecision.Deny("ELEVATED_SERVICE_REQUIRED", "Shizuku authority is not ready")
        }
        if (tool.requiresConfirmation && !userConfirmed) {
            return PolicyDecision.Deny("CONFIRMATION_REQUIRED", "Explicit per-action confirmation is required")
        }
        return PolicyDecision.Allow
    }
}

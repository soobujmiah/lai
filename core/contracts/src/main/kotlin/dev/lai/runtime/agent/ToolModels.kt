package dev.lai.runtime.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject = buildJsonObject { },
)

@Serializable
data class ToolError(
    val code: String,
    val message: String,
    val retryable: Boolean = false,
)

@Serializable
data class ToolResult(
    val callId: String,
    val success: Boolean,
    val output: JsonObject = buildJsonObject { },
    val error: ToolError? = null,
) {
    companion object {
        fun failure(callId: String, code: String, message: String, retryable: Boolean = false) = ToolResult(
            callId = callId,
            success = false,
            error = ToolError(code, message, retryable),
        )
    }
}

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val risk: ToolRisk,
    val requiresConfirmation: Boolean,
)

@Serializable
enum class ToolRisk { READ_ONLY, INTERACTION, SENSITIVE, ELEVATED }

package dev.lai.runtime.plugins

import dev.lai.runtime.agent.ToolCall
import dev.lai.runtime.agent.ToolResult
import kotlinx.serialization.json.JsonObject

enum class PluginCapability { CHAT_TOOL, OCR_POST_PROCESSOR, RAG_SOURCE, SPEECH_INPUT, SPEECH_OUTPUT }
enum class PluginRisk { LOW, INTERACTION, SENSITIVE, ELEVATED }
enum class PluginDataPolicy { LOCAL_ONLY }

data class PluginManifest(
    val id: String,
    val version: String,
    val apiVersion: Int,
    val description: String,
    val capabilities: Set<PluginCapability>,
    val risk: PluginRisk,
    val dataPolicy: PluginDataPolicy = PluginDataPolicy.LOCAL_ONLY,
    val inputSchema: String,
    val outputSchema: String,
    val signingKeyId: String? = null,
) {
    fun validate(): List<String> = buildList {
        if (!ID.matches(id)) add("Plugin id must use reverse-domain notation")
        if (!SEMVER.matches(version)) add("Plugin version must use semantic versioning")
        if (apiVersion != CURRENT_API_VERSION) add("Unsupported plugin API version")
        if (capabilities.isEmpty()) add("At least one capability is required")
        if (inputSchema.isBlank() || outputSchema.isBlank()) add("Input and output schemas are required")
        if (dataPolicy != PluginDataPolicy.LOCAL_ONLY) add("Only local-only plugins are accepted")
    }

    companion object {
        const val CURRENT_API_VERSION = 1
        private val ID = Regex("^[a-z][a-z0-9]*(?:\\.[a-z0-9][a-z0-9-]*){2,}$")
        private val SEMVER = Regex("^\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+)?$")
    }
}

interface PluginExecutionContext {
    suspend fun invokeApprovedTool(call: ToolCall): ToolResult
    fun reportProgress(fraction: Float, message: String)
}

data class PluginResult(val success: Boolean, val output: JsonObject, val error: String? = null)

interface LaiPlugin {
    val manifest: PluginManifest
    fun validateInput(input: JsonObject): List<String>
    suspend fun execute(input: JsonObject, context: PluginExecutionContext): PluginResult
}

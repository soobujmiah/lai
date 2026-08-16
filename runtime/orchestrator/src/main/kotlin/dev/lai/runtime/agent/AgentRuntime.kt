package dev.lai.runtime.agent

import dev.lai.runtime.automation.AccessibilityGateway
import dev.lai.runtime.automation.AutomationCommand
import dev.lai.runtime.automation.GlobalActionType
import dev.lai.runtime.automation.NodeSelector
import dev.lai.runtime.core.LaiJson
import dev.lai.runtime.ocr.BanglaOcrService
import dev.lai.runtime.shell.ElevatedShell
import dev.lai.runtime.shell.PrivilegedCommand
import dev.lai.runtime.shell.ShizukuController
import dev.lai.runtime.shell.ShizukuState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AgentRuntime(
    private val shell: ElevatedShell,
    private val shizuku: ShizukuController,
    private val ocr: BanglaOcrService,
    private val policy: AgentPolicy = AgentPolicy(),
) {
    val tools: List<ToolDefinition> = listOf(
        ToolDefinition("screen.snapshot", "Read the current accessibility tree", ToolRisk.READ_ONLY, false),
        ToolDefinition("screen.click", "Click a visible UI element", ToolRisk.INTERACTION, true),
        ToolDefinition("screen.type", "Enter text into an editable UI element", ToolRisk.SENSITIVE, true),
        ToolDefinition("screen.scroll", "Scroll a visible container", ToolRisk.INTERACTION, false),
        ToolDefinition("system.global_action", "Back, home, recents, or notifications", ToolRisk.INTERACTION, true),
        ToolDefinition("app.launch", "Launch an installed application", ToolRisk.INTERACTION, true),
        ToolDefinition("ocr.current_screen", "Capture and recognize current screen text", ToolRisk.READ_ONLY, false),
        ToolDefinition("shell.operation", "Run one structured, allowlisted Shizuku operation", ToolRisk.ELEVATED, true),
    )

    suspend fun execute(call: ToolCall, userConfirmed: Boolean = false): ToolResult = runCatching {
        val definition = tools.firstOrNull { it.name == call.name }
            ?: return ToolResult.failure(call.id, "unknown_tool", "Unknown tool: ${call.name}")
        val decision = policy.review(
            tool = definition,
            authority = AuthorityContext(
                accessibilityConnected = AccessibilityGateway.connected.value,
                elevatedServiceReady = shizuku.state.value is ShizukuState.Ready,
            ),
            userConfirmed = userConfirmed,
        )
        if (decision is PolicyDecision.Deny) {
            return ToolResult.failure(call.id, decision.code.lowercase(), decision.reason)
        }

        when (call.name) {
            "screen.snapshot" -> automation(call, AutomationCommand.Snapshot)
            "screen.click" -> automation(call, AutomationCommand.Click(call.arguments.selector()))
            "screen.type" -> automation(
                call,
                AutomationCommand.SetText(
                    selector = call.arguments.selector(),
                    text = call.arguments.string("text"),
                    allowSensitiveInput = call.arguments.boolean("allowSensitiveInput") ?: false,
                ),
            )
            "screen.scroll" -> automation(
                call,
                AutomationCommand.Scroll(
                    selector = call.arguments.takeIf { "selector" in it }?.selector("selector"),
                    forward = call.arguments.boolean("forward") ?: true,
                ),
            )
            "system.global_action" -> automation(
                call,
                AutomationCommand.GlobalAction(
                    GlobalActionType.valueOf(call.arguments.string("action").uppercase()),
                ),
            )
            "app.launch" -> automation(call, AutomationCommand.LaunchApp(call.arguments.string("package")))
            "ocr.current_screen" -> runOcr(call)
            "shell.operation" -> runShell(call, userConfirmed)
            else -> ToolResult.failure(call.id, "unknown_tool", "Unknown tool")
        }
    }.getOrElse {
        ToolResult.failure(call.id, "tool_error", it.message ?: "Tool failed")
    }

    private suspend fun automation(call: ToolCall, command: AutomationCommand): ToolResult {
        val result = AccessibilityGateway.execute(command)
        if (!result.success) return ToolResult.failure(call.id, "automation_failed", result.message, true)
        return ToolResult(
            callId = call.id,
            success = true,
            output = buildJsonObject {
                put("message", result.message)
                result.snapshot?.let {
                    put("snapshot", LaiJson.encodeToJsonElement(dev.lai.runtime.automation.ScreenSnapshot.serializer(), it))
                }
            },
        )
    }

    private suspend fun runOcr(call: ToolCall): ToolResult {
        val bitmap = AccessibilityGateway.captureScreen().getOrElse {
            return ToolResult.failure(call.id, "capture_failed", it.message ?: "Capture failed", true)
        }
        return try {
            val result = ocr.recognize(bitmap).getOrElse {
                return ToolResult.failure(call.id, "ocr_unavailable", it.message ?: "OCR failed")
            }
            ToolResult(
                callId = call.id,
                success = true,
                output = buildJsonObject {
                    put("ocr", LaiJson.encodeToJsonElement(dev.lai.runtime.ocr.OcrResult.serializer(), result))
                },
            )
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun runShell(call: ToolCall, confirmed: Boolean): ToolResult {
        val operation = call.arguments.string("operation")
        val arguments = call.arguments["arguments"]?.let { element ->
            (element as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }
        }.orEmpty()
        val result = shell.execute(PrivilegedCommand(operation, arguments), confirmed).getOrElse {
            return ToolResult.failure(call.id, "shell_rejected", it.message ?: "Shell operation rejected")
        }
        return ToolResult(
            callId = call.id,
            success = result.exitCode == 0 && !result.timedOut,
            output = buildJsonObject {
                put("exitCode", result.exitCode)
                put("stdout", result.stdout)
                put("stderr", result.stderr)
                put("timedOut", result.timedOut)
            },
            error = if (result.exitCode == 0 && !result.timedOut) null else {
                ToolError("shell_failed", "Privileged operation did not complete successfully")
            },
        )
    }

    private fun JsonObject.selector(key: String? = null): NodeSelector {
        val source = key?.let { this[it] as? JsonObject } ?: this
        return NodeSelector(
            viewId = source.optionalString("viewId"),
            text = source.optionalString("text"),
            contentDescription = source.optionalString("contentDescription"),
            path = source["path"]?.jsonArray?.map { it.jsonPrimitive.content.toInt() },
        ).also {
            require(it.viewId != null || it.text != null || it.contentDescription != null || it.path != null) {
                "A selector is required"
            }
        }
    }

    private fun JsonObject.string(key: String): String = optionalString(key)
        ?: error("Missing string argument: $key")

    private fun JsonObject.optionalString(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.boolean(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.booleanOrNull
}

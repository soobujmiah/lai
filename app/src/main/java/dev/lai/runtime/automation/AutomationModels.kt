package dev.lai.runtime.automation

import android.graphics.Rect
import kotlinx.serialization.Serializable

@Serializable
data class NodeSelector(
    val viewId: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val path: List<Int>? = null,
)

sealed interface AutomationCommand {
    data object Snapshot : AutomationCommand
    data class Click(val selector: NodeSelector) : AutomationCommand
    data class SetText(
        val selector: NodeSelector,
        val text: String,
        val allowSensitiveInput: Boolean = false,
    ) : AutomationCommand
    data class Scroll(val selector: NodeSelector?, val forward: Boolean) : AutomationCommand
    data class GlobalAction(val action: GlobalActionType) : AutomationCommand
    data class LaunchApp(val packageName: String) : AutomationCommand
}

enum class GlobalActionType { BACK, HOME, RECENTS, NOTIFICATIONS }

data class AutomationResult(
    val success: Boolean,
    val message: String,
    val snapshot: ScreenSnapshot? = null,
)

@Serializable
data class ScreenSnapshot(
    val packageName: String?,
    val windowTitle: String?,
    val capturedAtEpochMs: Long,
    val truncated: Boolean,
    val nodes: List<UiNode>,
)

@Serializable
data class UiNode(
    val path: List<Int>,
    val className: String?,
    val viewId: String?,
    val text: String?,
    val contentDescription: String?,
    val bounds: SerializableRect,
    val clickable: Boolean,
    val editable: Boolean,
    val scrollable: Boolean,
    val enabled: Boolean,
    val selected: Boolean,
    val password: Boolean,
)

@Serializable
data class SerializableRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    companion object {
        fun from(rect: Rect) = SerializableRect(rect.left, rect.top, rect.right, rect.bottom)
    }
}

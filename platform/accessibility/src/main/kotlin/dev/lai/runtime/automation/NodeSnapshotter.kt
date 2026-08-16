package dev.lai.runtime.automation

import android.view.accessibility.AccessibilityNodeInfo

internal object NodeSnapshotter {
    private const val MAX_NODES = 400
    private const val MAX_DEPTH = 24
    private const val MAX_TEXT_LENGTH = 500

    fun capture(root: AccessibilityNodeInfo, packageName: String?, title: String?): ScreenSnapshot {
        val nodes = ArrayList<UiNode>(128)
        var truncated = false

        fun visit(node: AccessibilityNodeInfo, path: List<Int>, depth: Int) {
            if (nodes.size >= MAX_NODES || depth > MAX_DEPTH) {
                truncated = true
                return
            }
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            nodes += UiNode(
                path = path,
                className = node.className?.toString(),
                viewId = node.viewIdResourceName,
                text = if (node.isPassword) null else node.text?.toString()?.take(MAX_TEXT_LENGTH),
                contentDescription = if (node.isPassword) null else {
                    node.contentDescription?.toString()?.take(MAX_TEXT_LENGTH)
                },
                bounds = SerializableRect(bounds.left, bounds.top, bounds.right, bounds.bottom),
                clickable = node.isClickable,
                editable = node.isEditable,
                scrollable = node.isScrollable,
                enabled = node.isEnabled,
                selected = node.isSelected,
                password = node.isPassword,
            )
            for (index in 0 until node.childCount) {
                if (nodes.size >= MAX_NODES) {
                    truncated = true
                    return
                }
                val child = node.getChild(index) ?: continue
                try {
                    visit(child, path + index, depth + 1)
                } finally {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
            }
        }

        visit(root, emptyList(), 0)
        return ScreenSnapshot(
            packageName = packageName,
            windowTitle = title,
            capturedAtEpochMs = System.currentTimeMillis(),
            truncated = truncated,
            nodes = nodes,
        )
    }
}

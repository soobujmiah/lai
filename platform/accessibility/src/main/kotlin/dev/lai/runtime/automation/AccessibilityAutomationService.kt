package dev.lai.runtime.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AccessibilityAutomationService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityGateway.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        AccessibilityGateway.updateForegroundPackage(event?.packageName?.toString())
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        AccessibilityGateway.detach(this)
        super.onDestroy()
    }

    fun execute(command: AutomationCommand): AutomationResult = when (command) {
        AutomationCommand.Snapshot -> snapshot()
        is AutomationCommand.Click -> withSelectedNode(command.selector) { node ->
            if (!node.isEnabled) return@withSelectedNode AutomationResult(false, "Target is disabled")
            val clickable = generateSequence(node) { it.parent }.take(7).firstOrNull { it.isClickable }
            val ok = clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            AutomationResult(ok, if (ok) "Clicked target" else "No clickable target found")
        }

        is AutomationCommand.SetText -> withSelectedNode(command.selector) { node ->
            if (node.isPassword && !command.allowSensitiveInput) {
                return@withSelectedNode AutomationResult(false, "Sensitive text input requires explicit approval")
            }
            if (!node.isEditable) return@withSelectedNode AutomationResult(false, "Target is not editable")
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, command.text)
            }
            val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            AutomationResult(ok, if (ok) "Text entered" else "App rejected text entry")
        }

        is AutomationCommand.Scroll -> {
            val root = rootInActiveWindow ?: return AutomationResult(false, "No active window")
            try {
                val target = command.selector?.let { findNode(root, it) }
                    ?: findFirst(root) { it.isScrollable }
                val action = if (command.forward) {
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                } else {
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                }
                val ok = target?.performAction(action) == true
                AutomationResult(ok, if (ok) "Scrolled" else "No scrollable target found")
            } finally {
                @Suppress("DEPRECATION")
                root.recycle()
            }
        }

        is AutomationCommand.GlobalAction -> {
            val androidAction = when (command.action) {
                GlobalActionType.BACK -> GLOBAL_ACTION_BACK
                GlobalActionType.HOME -> GLOBAL_ACTION_HOME
                GlobalActionType.RECENTS -> GLOBAL_ACTION_RECENTS
                GlobalActionType.NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
            }
            val ok = performGlobalAction(androidAction)
            AutomationResult(ok, if (ok) "Global action sent" else "Global action failed")
        }

        is AutomationCommand.LaunchApp -> launchApp(command.packageName)
    }

    private fun snapshot(): AutomationResult {
        val root = rootInActiveWindow ?: return AutomationResult(false, "No active window")
        return try {
            val activeWindow = windows.firstOrNull { it.isActive }
            val result = NodeSnapshotter.capture(
                root = root,
                packageName = root.packageName?.toString(),
                title = activeWindow?.title?.toString(),
            )
            AutomationResult(true, "Captured ${result.nodes.size} interface nodes", result)
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun withSelectedNode(
        selector: NodeSelector,
        action: (AccessibilityNodeInfo) -> AutomationResult,
    ): AutomationResult {
        val root = rootInActiveWindow ?: return AutomationResult(false, "No active window")
        return try {
            val node = findNode(root, selector) ?: return AutomationResult(false, "Target not found")
            action(node)
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun findNode(root: AccessibilityNodeInfo, selector: NodeSelector): AccessibilityNodeInfo? {
        selector.viewId?.let { id ->
            root.findAccessibilityNodeInfosByViewId(id).firstOrNull()?.let { return it }
        }
        selector.path?.let { path ->
            var current = root
            path.forEach { index ->
                current = current.getChild(index) ?: return@let
            }
            return current
        }
        selector.text?.let { text ->
            root.findAccessibilityNodeInfosByText(text).firstOrNull {
                it.text?.toString() == text || it.contentDescription?.toString() == text
            }?.let { return it }
        }
        selector.contentDescription?.let { description ->
            return findFirst(root) { it.contentDescription?.toString() == description }
        }
        return null
    }

    private fun findFirst(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            findFirst(child, predicate)?.let { return it }
        }
        return null
    }

    private fun launchApp(packageName: String): AutomationResult {
        if (!PACKAGE_NAME.matches(packageName)) return AutomationResult(false, "Invalid package name")
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return AutomationResult(false, "No launchable activity for $packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            startActivity(intent)
            AutomationResult(true, "Launched $packageName")
        }.getOrElse { AutomationResult(false, it.message ?: "Launch failed") }
    }

    suspend fun captureScreen(): Result<Bitmap> {
        if (android.os.Build.VERSION.SDK_INT < 30) {
            return Result.failure(UnsupportedOperationException("Screen capture requires Android 11+"))
        }
        return suspendCancellableCoroutine { continuation ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val buffer = screenshot.hardwareBuffer
                        val bitmap = try {
                            Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                                ?.copy(Bitmap.Config.ARGB_8888, false)
                        } finally {
                            buffer.close()
                        }
                        continuation.resume(
                            bitmap?.let(Result.Companion::success)
                                ?: Result.failure(IllegalStateException("Could not map screenshot buffer")),
                        )
                    }

                    override fun onFailure(errorCode: Int) {
                        continuation.resume(Result.failure(IllegalStateException("Screenshot failed: $errorCode")))
                    }
                },
            )
        }
    }

    companion object {
        private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+$")
    }
}

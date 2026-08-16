package dev.lai.runtime.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationContractsTest {
    @Test
    fun `snapshot and commands are immutable transport values`() {
        val selector = NodeSelector(viewId = "pkg:id/button")
        assertEquals(selector, (AutomationCommand.Click(selector) as AutomationCommand.Click).selector)
        assertEquals(GlobalActionType.BACK, AutomationCommand.GlobalAction(GlobalActionType.BACK).action)
        assertTrue(AutomationCommand.Scroll(null, true).forward)
        assertFalse(AutomationCommand.SetText(selector, "বাংলা").allowSensitiveInput)
        assertEquals("pkg", AutomationCommand.LaunchApp("pkg").packageName)
        assertTrue(AutomationCommand.Snapshot is AutomationCommand)

        val rect = SerializableRect(1, 2, 3, 4)
        val node = UiNode(
            path = listOf(0),
            className = "Button",
            viewId = "pkg:id/button",
            text = "OK",
            contentDescription = null,
            bounds = rect,
            clickable = true,
            editable = false,
            scrollable = false,
            enabled = true,
            selected = false,
            password = false,
        )
        val snapshot = ScreenSnapshot("pkg", "title", 1, false, listOf(node))
        assertEquals(rect, snapshot.nodes.single().bounds)
        assertTrue(AutomationResult(true, "ok", snapshot).success)
    }
}

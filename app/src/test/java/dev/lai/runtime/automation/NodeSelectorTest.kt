package dev.lai.runtime.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class NodeSelectorTest {
    @Test
    fun `selector preserves deterministic node path`() {
        val selector = NodeSelector(path = listOf(0, 2, 1))
        assertEquals(listOf(0, 2, 1), selector.path)
    }
}

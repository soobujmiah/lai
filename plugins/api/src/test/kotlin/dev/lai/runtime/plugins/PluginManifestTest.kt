package dev.lai.runtime.plugins

import org.junit.Assert.assertTrue
import org.junit.Test

class PluginManifestTest {
    @Test
    fun `valid local plugin manifest passes`() {
        val manifest = PluginManifest(
            id = "dev.lai.skills.calendar",
            version = "1.0.0",
            apiVersion = PluginManifest.CURRENT_API_VERSION,
            description = "Local calendar skill",
            capabilities = setOf(PluginCapability.CHAT_TOOL),
            risk = PluginRisk.SENSITIVE,
            inputSchema = "{}",
            outputSchema = "{}",
        )
        assertTrue(manifest.validate().isEmpty())
    }
}

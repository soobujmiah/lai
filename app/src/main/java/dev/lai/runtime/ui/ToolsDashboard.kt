package dev.lai.runtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lai.runtime.agent.ToolRisk

/** Clean, minimal card for one tool — no technical bloat for end users. */
data class DashboardTool(
    val name: String,
    val title: String,
    val description: String,
    val risk: ToolRisk,
    val requiresConfirmation: Boolean,
    val category: String,
)

private val dashboardTools = listOf(
    DashboardTool("screen.snapshot", "Read screen", "Capture the visible accessibility tree (text omitted for passwords)", ToolRisk.READ_ONLY, false, "Vision"),
    DashboardTool("ocr.current_screen", "Recognize text", "Local OCR placeholder — structured JSON, no cloud", ToolRisk.READ_ONLY, false, "Vision"),
    DashboardTool("screen.click", "Tap control", "Click a button or field by id / text / path", ToolRisk.INTERACTION, true, "Interaction"),
    DashboardTool("screen.type", "Enter text", "Type into a focused field — sensitive text requires approval", ToolRisk.SENSITIVE, true, "Interaction"),
    DashboardTool("screen.scroll", "Scroll", "Scroll the visible container forward or backward", ToolRisk.INTERACTION, false, "Interaction"),
    DashboardTool("system.global_action", "System action", "Back, Home, Recents, Notifications", ToolRisk.INTERACTION, true, "Interaction"),
    DashboardTool("app.launch", "Launch app", "Open an installed app by package name", ToolRisk.INTERACTION, true, "Interaction"),
    DashboardTool("shell.operation", "Elevated operation", "Single allowlisted Shizuku operation (no raw shell)", ToolRisk.ELEVATED, true, "Elevated"),
)

@Composable
fun ToolsDashboard(state: MainUiState, viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tools dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Every action needs your approval first. No tool runs on its own.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val grouped = dashboardTools.groupBy { it.category }
        for ((category, tools) in grouped) {
            Text(category, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            for (tool in tools) {
                ToolCard(tool, state, viewModel)
            }
        }
        if (state.toolAuditHistory.isNotEmpty()) {
            Text("Recent local audit", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            Text(
                "${state.toolAuditHistory.size} event(s) • latest ${state.toolAuditHistory.last().toolName} • ${state.toolAuditStatus}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ToolCard(tool: DashboardTool, state: MainUiState, viewModel: MainViewModel) {
    val riskLabel = when (tool.risk) {
        ToolRisk.READ_ONLY -> "Read-only"
        ToolRisk.INTERACTION -> "Interaction"
        ToolRisk.SENSITIVE -> "Sensitive"
        ToolRisk.ELEVATED -> "Elevated"
    }
    val riskColor = when (tool.risk) {
        ToolRisk.READ_ONLY -> MaterialTheme.colorScheme.secondary
        ToolRisk.INTERACTION -> MaterialTheme.colorScheme.primary
        ToolRisk.SENSITIVE -> MaterialTheme.colorScheme.error
        ToolRisk.ELEVATED -> MaterialTheme.colorScheme.error
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tool.title, fontWeight = FontWeight.SemiBold)
                Text(riskLabel, style = MaterialTheme.typography.labelSmall, color = riskColor)
            }
            Text(tool.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (tool.requiresConfirmation) "Requires your tap to confirm" else "Runs after screen access is on",
                style = MaterialTheme.typography.labelSmall,
            )
            // One-tap demo for the two safe, device-free actions
            when (tool.name) {
                "screen.snapshot" -> OutlinedButton(
                    onClick = viewModel::inspectScreen,
                    enabled = !state.busy && state.accessibilityConnected,
                ) { Text("Inspect visible screen") }
                "ocr.current_screen" -> OutlinedButton(
                    onClick = { viewModel.readCurrentScreen() },
                    enabled = !state.busy && state.accessibilityConnected,
                ) { Text("Run local OCR") }
                else -> Text("Use from Chat — ask LAI to ${tool.title.lowercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

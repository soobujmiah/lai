package dev.lai.runtime.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lai.runtime.ui.MainUiState

@Composable
fun LaiWorkspaceScreen(
    state: MainUiState,
    onGrantWorkspace: (android.net.Uri) -> Unit,
    onRevokeWorkspace: () -> Unit,
    onRefreshWorkspace: () -> Unit,
    onScanWorkspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspace = state.toWorkspaceUi()
    val workspacePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(onGrantWorkspace) }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Connect a user-owned SAF folder for settings and bounded model discovery. Paths and filenames are not exposed here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(workspace.grantStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(workspace.settingsStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (workspace.granted) {
                        OutlinedButton(onClick = onRevokeWorkspace, enabled = !workspace.busy) { Text("Disconnect") }
                        OutlinedButton(onClick = onRefreshWorkspace, enabled = !workspace.busy) { Text("Refresh") }
                    } else {
                        Button(onClick = { workspacePicker.launch(null) }, enabled = !workspace.busy) { Text("Grant workspace") }
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Model discovery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Scanning is bounded and metadata-only. Reviewed hashes are counted separately from local unreviewed files; nothing is auto-loaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Reviewed: ${workspace.reviewedModelCount} • Local unreviewed: ${workspace.localUnreviewedModelCount}")
                Text(workspace.discoveryStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (workspace.discovering) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = onScanWorkspace,
                    enabled = workspace.granted && !workspace.busy && !workspace.discovering,
                ) { Text(if (workspace.discovering) "Scanning…" else "Scan workspace models") }
            }
        }
        state.notice?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

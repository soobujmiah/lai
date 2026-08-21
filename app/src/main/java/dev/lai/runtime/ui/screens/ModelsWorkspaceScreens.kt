package dev.lai.runtime.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModelsScreen(
    models: List<LaiModelUi>,
    activeModelId: String?,
    busy: Boolean,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onInstallRecommended: () -> Unit,
    onRefreshCatalog: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Models", style = MaterialTheme.typography.headlineSmall)
        Text("Local models and the verified offline catalog", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(models, key = { it.id }) { model ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(model.name, style = MaterialTheme.typography.titleMedium)
                        Text(model.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(model.status, style = MaterialTheme.typography.labelMedium)
                        if (model.id == activeModelId) {
                            Text("Active", color = MaterialTheme.colorScheme.primary)
                        } else {
                            Button(enabled = !busy, onClick = { onLoad(model.id) }) { Text("Load") }
                        }
                        OutlinedButton(enabled = !busy && model.id != activeModelId, onClick = { onDelete(model.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
        Button(enabled = !busy, onClick = onInstallRecommended) { Text("Install recommended") }
        OutlinedButton(enabled = !busy, onClick = onRefreshCatalog) { Text("Refresh catalog") }
    }
}

@Composable
fun WorkspaceScreen(
    projects: List<LaiProjectUi>,
    granted: Boolean,
    busy: Boolean,
    onGrant: () -> Unit,
    onRevoke: () -> Unit,
    onScan: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Workspace", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Models are discovered from the user-approved workspace; raw files stay behind the workspace authority boundary.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        projects.forEach { project ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    Text(project.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (granted) {
            Button(enabled = !busy, onClick = onScan) { Text("Scan for models") }
            OutlinedButton(enabled = !busy, onClick = onRevoke) { Text("Disconnect workspace") }
        } else {
            Button(enabled = !busy, onClick = onGrant) { Text("Connect workspace") }
        }
    }
}

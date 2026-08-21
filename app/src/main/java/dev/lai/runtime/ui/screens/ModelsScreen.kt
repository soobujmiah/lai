package dev.lai.runtime.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lai.runtime.ui.MainUiState

/** Real model catalog surface backed only by MainUiState and MainViewModel actions. */
@Composable
fun LaiModelsScreen(
    state: MainUiState,
    onRefreshSupported: () -> Unit,
    onInstallRecommended: () -> Unit,
    onInstall: (String) -> Unit,
    onLoad: (String) -> Unit,
    onUnload: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val models = state.toModelsUi()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Models", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Install reviewed models, load exactly one local runtime model, and keep CPU fallback visible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RuntimeModelSummary(state = state, onUnload = onUnload)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRefreshSupported,
                        enabled = !state.catalogRefreshing && !state.busy,
                    ) { Text(if (state.catalogRefreshing) "Checking…" else "Refresh catalog") }
                    Button(
                        onClick = onInstallRecommended,
                        enabled = !state.busy && state.installedModels.none { it.id == state.recommendedModel.id },
                    ) { Text("Install recommended") }
                }
            }
        }
        if (models.isEmpty()) {
            item { EmptyModelsCard() }
        }
        items(models, key = { it.id }) { model ->
            ModelCard(
                model = model,
                onInstall = { onInstall(model.id) },
                onLoad = { onLoad(model.id) },
                onDelete = { onDelete(model.id) },
            )
        }
        state.notice?.let { notice ->
            item { Text(notice, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun RuntimeModelSummary(state: MainUiState, onUnload: () -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Runtime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        state.schedulerDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.activeModelId != null) {
                    OutlinedButton(onClick = onUnload, enabled = !state.busy) { Text("Unload") }
                }
            }
            Text(
                state.runtimeDetail.ifBlank { "Native runtime status unavailable" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.downloadProgress?.fraction?.let { fraction ->
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelCard(
    model: LaiModelUi,
    onInstall: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (model.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(model.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = {}, label = { Text(model.status) })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(model.format) })
                AssistChip(onClick = {}, label = { Text(model.quantization) })
                AssistChip(onClick = {}, label = { Text(model.sizeLabel) })
                if (model.recommended) AssistChip(onClick = {}, label = { Text("Recommended") })
            }
            Text(model.backendLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(model.reviewLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            model.progress?.fraction?.let { fraction ->
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (model.installed) {
                    Button(onClick = onLoad, enabled = model.loadEnabled) { Text(if (model.loading) "Loading…" else if (model.active) "Loaded" else "Load") }
                    OutlinedButton(onClick = onDelete, enabled = model.deleteEnabled) { Text("Delete") }
                } else {
                    Button(onClick = onInstall, enabled = model.installEnabled) { Text(if (model.installing) "Installing…" else "Download") }
                }
            }
        }
    }
}

@Composable
private fun EmptyModelsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("No models listed", style = MaterialTheme.typography.titleMedium)
            Text(
                "Refresh the signed catalog or connect a workspace. LAI will not load or scan broad storage without permission.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package dev.lai.runtime.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Model catalog presentation surface; loading/import/runtime actions stay in the ViewModel. */
@Composable
fun LaiModelsScreen(
    models: List<LaiModelUi> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Models", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Manage local models and their runtime readiness.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(models, key = { it.id }) { model ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(model.name, style = MaterialTheme.typography.titleMedium)
                        Text(model.detail, style = MaterialTheme.typography.bodyMedium)
                        AssistChip(onClick = {}, label = { Text(model.status) })
                    }
                }
            }
        }
    }
}

data class LaiModelUi(
    val id: String,
    val name: String,
    val detail: String,
    val status: String,
)

package dev.lai.runtime.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Provider presentation model. Qualification is explicit: presence alone never means ready. */
data class LaiProviderUi(
    val id: String,
    val name: String,
    val kind: String,
    val status: String,
    val detail: String,
)

@Composable
fun LaiProvidersScreen(
    providers: List<LaiProviderUi> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("AI Providers", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Local and remote providers are governed by explicit capability and policy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(providers, key = { it.id }) { provider ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(provider.name, style = MaterialTheme.typography.titleMedium)
                        Text("${provider.kind} · ${provider.status}", style = MaterialTheme.typography.labelMedium)
                        Text(provider.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

package dev.lai.runtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lai.runtime.R
import dev.lai.runtime.shell.ShizukuState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaiApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LAI", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = viewModel::toggleSettings) {
                        Text(stringResource(R.string.settings))
                    }
                },
            )
        },
        bottomBar = {
            if (!state.settingsVisible) {
                NavigationBar {
                    ModeNavigationItem(UiMode.CHAT, state.mode, stringResource(R.string.chat), viewModel::setMode)
                    ModeNavigationItem(
                        UiMode.SCREEN_READER,
                        state.mode,
                        stringResource(R.string.screen_reader),
                        viewModel::setMode,
                    )
                    ModeNavigationItem(
                        UiMode.AUTOMATOR,
                        state.mode,
                        stringResource(R.string.automator),
                        viewModel::setMode,
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.settingsVisible -> SettingsScreen(state, viewModel)
                state.mode == UiMode.CHAT -> ChatScreen(state, viewModel)
                state.mode == UiMode.SCREEN_READER -> ScreenReaderScreen(state, viewModel)
                else -> AutomatorScreen(state, viewModel)
            }
            if (state.busy && state.downloadProgress == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun RowScope.ModeNavigationItem(
    mode: UiMode,
    selected: UiMode,
    label: String,
    onSelect: (UiMode) -> Unit,
) {
    NavigationBarItem(
        selected = mode == selected,
        onClick = { onSelect(mode) },
        icon = { Text(when (mode) { UiMode.CHAT -> "●"; UiMode.SCREEN_READER -> "◉"; UiMode.AUTOMATOR -> "◆" }) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun ChatScreen(state: MainUiState, viewModel: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.messages) { message -> MessageBubble(message) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::setInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.message_hint)) },
                maxLines = 4,
                keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
            )
            Button(onClick = viewModel::sendMessage, enabled = state.input.isNotBlank() && !state.busy) {
                Text(stringResource(R.string.send))
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.86f),
            colors = CardDefaults.cardColors(
                containerColor = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(message.text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ScreenReaderScreen(state: MainUiState, viewModel: MainViewModel) {
    FeatureScreen(
        title = stringResource(R.string.screen_reader),
        subtitle = "Capture the current screen privately and return structured Bangla/English OCR JSON.",
        state = state,
    ) {
        if (!state.accessibilityConnected) {
            Button(onClick = viewModel::openAccessibilitySettings) { Text(stringResource(R.string.enable_accessibility)) }
        } else {
            Button(onClick = viewModel::readCurrentScreen, enabled = !state.busy) {
                Text(stringResource(R.string.read_screen))
            }
        }
        StatusCard(
            title = if (state.accessibilityConnected) "Screen access ready" else "Screen access is off",
            detail = if (state.accessibilityConnected) "Screenshots stay on this device."
            else "Android requires you to enable LAI automation first.",
        )
    }
}

@Composable
private fun AutomatorScreen(state: MainUiState, viewModel: MainViewModel) {
    FeatureScreen(
        title = stringResource(R.string.automator),
        subtitle = "Understand visible controls and perform explicitly approved actions.",
        state = state,
    ) {
        if (!state.accessibilityConnected) {
            Button(onClick = viewModel::openAccessibilitySettings) { Text(stringResource(R.string.enable_accessibility)) }
        } else {
            Button(onClick = viewModel::inspectScreen, enabled = !state.busy) {
                Text(stringResource(R.string.inspect_screen))
            }
        }
        val shizukuText = when (val value = state.shizukuState) {
            ShizukuState.Unavailable -> "Shizuku is not running"
            ShizukuState.PermissionRequired -> "Shizuku permission required"
            is ShizukuState.Ready -> "Shizuku ready (UID ${value.uid})"
            is ShizukuState.Error -> value.message
        }
        StatusCard("Elevated controls", shizukuText)
        if (state.shizukuState is ShizukuState.PermissionRequired) {
            OutlinedButton(onClick = viewModel::requestShizuku) { Text(stringResource(R.string.connect_shizuku)) }
        }
        StatusCard(stringResource(R.string.safety_title), stringResource(R.string.safety_body))
    }
}

@Composable
private fun FeatureScreen(
    title: String,
    subtitle: String,
    state: MainUiState,
    content: @Composable ColumnScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
        state.notice?.let { notice -> item { StatusCard("Result", notice) } }
    }
}

@Composable
private fun SettingsScreen(state: MainUiState, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.developer_mode), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.developer_mode_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.developerMode, onCheckedChange = viewModel::setDeveloperMode)
                }
            }
        }
        if (state.developerMode) {
            item { StatusCard("Native runtime", state.runtimeDetail) }
            item { StatusCard("Scheduler", state.schedulerDetail) }
            item {
                StatusCard(
                    "Device environment",
                    state.environmentDetail + (state.estimatedPeakBytes?.let {
                        " • estimated model peak ${it / 1_048_576} MB"
                    } ?: ""),
                )
            }
            item { ModelSetup(state, viewModel) }
        }
        state.notice?.let { notice -> item { StatusCard("Status", notice) } }
    }
}

@Composable
private fun ModelSetup(state: MainUiState, viewModel: MainViewModel) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.model_setup), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Models download directly to app-private storage. Every artifact requires a reviewed SHA-256.",
                style = MaterialTheme.typography.bodySmall,
            )
            val recommended = state.recommendedModel
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recommended baseline", style = MaterialTheme.typography.labelLarge)
                    Text(recommended.displayName, fontWeight = FontWeight.Bold)
                    Text(recommended.description, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${recommended.bytes / 1_048_576} MB • ${recommended.quantization} • ${recommended.license}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Button(
                        onClick = viewModel::installRecommendedModel,
                        enabled = !state.busy && state.installedModels.none { it.id == recommended.id },
                    ) {
                        Text(if (state.installedModels.any { it.id == recommended.id }) "Installed" else "Download securely")
                    }
                }
            }
            if (state.installedModels.isNotEmpty()) {
                Text("Installed", fontWeight = FontWeight.SemiBold)
                state.installedModels.forEach { model ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(model.displayName, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${model.bytes / 1_048_576} MB • ${model.sha256.take(12)}…",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        if (state.activeModelId == model.id) {
                            OutlinedButton(onClick = viewModel::unloadModel, enabled = !state.busy) {
                                Text("Unload")
                            }
                        } else {
                            Button(onClick = { viewModel.loadModel(model.id) }, enabled = !state.busy) {
                                Text("Load")
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
            OutlinedTextField(
                value = state.modelName,
                onValueChange = viewModel::setModelName,
                label = { Text(stringResource(R.string.model_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.modelUrl,
                onValueChange = viewModel::setModelUrl,
                label = { Text(stringResource(R.string.model_url)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = state.modelSha,
                onValueChange = viewModel::setModelSha,
                label = { Text(stringResource(R.string.model_sha)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            state.downloadProgress?.let { progress ->
                progress.fraction?.let { fraction ->
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("${progress.downloadedBytes / 1_048_576} MB", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = viewModel::downloadModel,
                enabled = !state.busy && state.modelName.isNotBlank() && state.modelUrl.isNotBlank() &&
                    state.modelSha.matches(Regex("^[a-fA-F0-9]{64}$")),
            ) { Text(stringResource(R.string.download)) }
        }
    }
}

@Composable
private fun StatusCard(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

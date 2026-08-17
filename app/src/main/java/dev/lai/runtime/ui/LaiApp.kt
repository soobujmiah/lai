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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lai.runtime.R
import dev.lai.runtime.agent.ToolRisk
import dev.lai.runtime.shell.ShizukuState
import dev.lai.runtime.workspace.WorkspaceGrantState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaiApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    // Hardware/gesture back must leave Settings as well; otherwise back exits the whole app from
    // a screen the user thinks of as "inside" something.
    BackHandler(enabled = state.settingsVisible) { viewModel.toggleSettings() }
    Scaffold(
        // The activity is edge-to-edge and resizes for the keyboard. Without an explicit contentWindowInsets
        // the Scaffold keeps reserving the status-bar inset while the IME is up, which pushed the whole
        // layout upward and made the top bar collide with the status bar (field report, Redmi Turbo 4 Pro).
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("LAI", fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                navigationIcon = {
                    // Settings is a full screen, not a mode. It needs an unambiguous way out:
                    // reusing the same "Settings" button to leave was a guess the user had to make.
                    if (state.settingsVisible) {
                        IconButton(onClick = viewModel::toggleSettings) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                actions = {
                    // Contextual quick settings belong to Chat only; other modes have no LLM knobs.
                    if (state.mode == UiMode.CHAT && !state.settingsVisible) {
                        // Glyph rather than a vector: material-icons-extended would add ~9 MB of
                        // unused vectors to keep the debug APK honest about its size.
                        IconButton(onClick = viewModel::showQuickSettings) {
                            Text(
                                stringResource(R.string.quick_settings_action),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    if (!state.settingsVisible) {
                        TextButton(onClick = viewModel::toggleSettings) {
                            Text(stringResource(R.string.settings))
                        }
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
            if (
                state.busy &&
                state.downloadProgress == null &&
                state.operation !in setOf(RuntimeOperation.GENERATING, RuntimeOperation.CANCELLING)
            ) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
    if (state.workspace.quickSettingsVisible) {
        QuickSettingsSheet(
            current = state.workspace.effectiveLlm,
            maxNewTokensCeiling = viewModel.maxNewTokensCeiling(),
            statusLine = state.workspace.settingsStatus,
            overrideArmed = state.workspace.overrideArmed,
            saving = state.workspace.savingSettings,
            onApplyOnce = viewModel::applyQuickSettings,
            onSaveDefault = { llm ->
                viewModel.saveDefaultSettings(state.workspace.session.saved.copy(llm = llm))
            },
            onReset = viewModel::resetSettings,
            onDismiss = viewModel::hideQuickSettings,
        )
    }
    state.pendingToolProposal?.let { proposal ->
        ToolConfirmationDialog(
            proposal = proposal,
            onApprove = viewModel::approvePendingTool,
            onDeny = viewModel::denyPendingTool,
        )
    }
}

@Composable
private fun ToolConfirmationDialog(
    proposal: PendingToolProposal,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text(stringResource(R.string.review_local_action)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(proposal.summary, fontWeight = FontWeight.SemiBold)
                Text("${stringResource(R.string.risk_label)}: ${toolRiskLabel(proposal.risk)}")
                Text(
                    stringResource(R.string.tool_review_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onApprove) { Text(stringResource(R.string.approve_once)) } },
        dismissButton = { TextButton(onClick = onDeny) { Text(stringResource(R.string.do_not_run)) } },
    )
}

@Composable
private fun toolRiskLabel(risk: ToolRisk): String = stringResource(
    when (risk) {
        ToolRisk.READ_ONLY -> R.string.risk_read_only
        ToolRisk.INTERACTION -> R.string.risk_interaction
        ToolRisk.SENSITIVE -> R.string.risk_sensitive
        ToolRisk.ELEVATED -> R.string.risk_elevated
    },
)

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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = viewModel::clearConversation,
                enabled = !state.busy && state.pendingToolProposal == null && state.messages.any { it.contextEligible },
            ) { Text("New chat") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.messages) { message -> MessageBubble(message) }
        }
        Row(
            // No imePadding() here: contentWindowInsets = safeDrawing already includes the IME
            // inset, so adding it again shifted the composer up by the keyboard height twice and
            // pushed it off screen (field report, build 0.6.83).
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
            if (state.operation in setOf(RuntimeOperation.GENERATING, RuntimeOperation.CANCELLING)) {
                Button(
                    onClick = viewModel::cancelGeneration,
                    enabled = state.operation == RuntimeOperation.GENERATING,
                ) {
                    Text(if (state.operation == RuntimeOperation.CANCELLING) "Stopping…" else "Stop")
                }
            } else {
                Button(
                    onClick = viewModel::sendMessage,
                    enabled = state.input.isNotBlank() && !state.busy && state.pendingToolProposal == null,
                ) {
                    Text(stringResource(R.string.send))
                }
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
    val diagnosticsExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let(viewModel::exportDiagnostics)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            StatusCard(
                title = "Local-first privacy",
                detail = "Internet is used only for the signed model catalog and explicit downloads. Prompts, screens, generations and telemetry stay on this device.",
            )
        }
        item {
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.local_action_proposals), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.local_action_proposals_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.toolProposalsEnabled,
                        onCheckedChange = viewModel::setToolProposalsEnabled,
                        enabled = !state.busy && state.pendingToolProposal == null && state.toolAuditIntegrityValid,
                    )
                }
            }
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
                val counters = state.toolProposalCounters
                StatusCard(
                    "Tool proposal parser",
                    "${counters.responsesExamined} examined • ${counters.accepted} accepted • " +
                        "${counters.rejected} rejected • ${counters.notToolCall} ordinary response(s) • " +
                        "last ${counters.lastOutcome}",
                )
            }
            item {
                val latest = state.toolAuditHistory.lastOrNull()
                StatusCard(
                    "Local tool audit",
                    if (latest == null) {
                        state.toolAuditStatus
                    } else {
                        "${state.toolAuditHistory.size} recent persistent event(s) • latest ${latest.toolName} • " +
                            "${latest.outcome.name.lowercase().replace('_', ' ')} • ${state.toolAuditStatus}"
                    },
                )
            }
            item {
                StatusCard(
                    "Device environment",
                    state.environmentDetail + (state.estimatedPeakBytes?.let {
                        " • estimated model peak ${it / 1_048_576} MB"
                    } ?: ""),
                )
            }
            state.lastModelLoadMs?.let { loadMs ->
                item { StatusCard("Model load", "$loadMs ms • local ${state.activeModelId ?: "session"}") }
            }
            state.lastGenerationMetrics?.let { metrics ->
                item {
                    StatusCard(
                        "Last generation",
                        "${metrics.promptTokens} prompt • ${metrics.generatedTokens} output • " +
                            "TTFT ${metrics.timeToFirstTokenMs} ms • " +
                            "prefill ${String.format(Locale.US, "%.2f", metrics.promptTokensPerSecond)} tok/s • " +
                            "decode ${String.format(Locale.US, "%.2f", metrics.decodeTokensPerSecond)} tok/s • " +
                            "total ${metrics.totalMs} ms" +
                            if (state.trimmedConversationTurns > 0) {
                                " • trimmed ${state.trimmedConversationTurns} old turn(s)"
                            } else "",
                    )
                }
            }
        }
        item { WorkspaceCard(state, viewModel) }
        item { ModelSetup(state, viewModel) }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Support diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Export versioned JSON with device, scheduler, model and local performance data. " +
                            "Prompts, responses, screens, OCR text, documents and credentials are excluded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(state.diagnosticsStatus, style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(
                        onClick = { diagnosticsExporter.launch("lai-diagnostics-v1.json") },
                        enabled = !state.busy,
                    ) { Text("Export diagnostics JSON") }
                }
            }
        }
        state.notice?.let { notice -> item { StatusCard("Status", notice) } }
    }
}

/**
 * Workspace folder status and controls (Phase 2A item 7).
 *
 * The grant is taken with `ACTION_OPEN_DOCUMENT_TREE`, so the user picks exactly one folder and
 * LAI never asks for broad storage access. Only coarse counts are shown here - never a file name,
 * a path, or a digest.
 */
@Composable
private fun WorkspaceCard(state: MainUiState, viewModel: MainViewModel) {
    val workspacePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::grantWorkspace) }
    val workspace = state.workspace
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.workspace_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.workspace_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                when (workspace.grantState) {
                    WorkspaceGrantState.GRANTED -> stringResource(R.string.workspace_connected)
                    WorkspaceGrantState.REVOKED -> stringResource(R.string.workspace_revoked)
                    WorkspaceGrantState.NOT_GRANTED -> stringResource(R.string.workspace_not_connected)
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text(workspace.settingsStatus, style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (workspace.granted) {
                    OutlinedButton(onClick = viewModel::revokeWorkspace, enabled = !state.busy) {
                        Text(stringResource(R.string.workspace_disconnect))
                    }
                    Button(
                        onClick = viewModel::scanWorkspaceModels,
                        enabled = !state.busy && !workspace.discovering,
                    ) {
                        Text(
                            if (workspace.discovering) stringResource(R.string.workspace_scanning)
                            else stringResource(R.string.workspace_scan),
                        )
                    }
                } else {
                    Button(onClick = { workspacePicker.launch(null) }, enabled = !state.busy) {
                        Text(stringResource(R.string.workspace_connect))
                    }
                }
            }
            if (workspace.granted) {
                Text(
                    stringResource(
                        R.string.workspace_counts,
                        workspace.reviewedModelCount,
                        workspace.localUnreviewedModelCount,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(workspace.discoveryStatus, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ModelSetup(state: MainUiState, viewModel: MainViewModel) {
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importRecommendedModel)
    }
    val pendingExportModelId = remember { mutableStateOf<String?>(null) }
    val modelExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val modelId = pendingExportModelId.value
        if (uri != null && modelId != null) viewModel.exportInstalledModel(modelId, uri)
        pendingExportModelId.value = null
    }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.model_setup), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Browse the signed supported-model list, download explicitly, then use everything offline. Local file import is also available.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Supported models", fontWeight = FontWeight.Bold)
                    Text(state.catalogStatus, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = viewModel::refreshSupportedModels,
                    enabled = !state.catalogRefreshing && !state.busy,
                ) { Text(if (state.catalogRefreshing) "Checking…" else "Refresh") }
            }
            state.supportedModels.forEach { supported ->
                val isRecommended = supported.id == state.recommendedModel.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecommended) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isRecommended) Text("Recommended baseline", style = MaterialTheme.typography.labelLarge)
                        Text(supported.displayName, fontWeight = FontWeight.Bold)
                        Text(supported.description, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${supported.bytes / 1_048_576} MB • ${supported.quantization} • ${supported.license}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        val isInstalled = state.installedModels.any { it.id == supported.id }
                        if (isInstalled) {
                            Button(onClick = {}, enabled = false) { Text("Installed") }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.installSupportedModel(supported.id) },
                                    enabled = !state.busy,
                                ) { Text("Download") }
                                if (isRecommended) {
                                    OutlinedButton(
                                        onClick = { modelPicker.launch(arrayOf("*/*")) },
                                        enabled = !state.busy,
                                    ) { Text("Import file") }
                                }
                            }
                        }
                    }
                }
            }
            if (state.installedModels.isNotEmpty()) {
                Text("Installed", fontWeight = FontWeight.SemiBold)
                Text(
                    "Use Keep copy to save a verified GGUF in Documents/Downloads. That copy survives app uninstall and can be imported after reinstall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    pendingExportModelId.value = model.id
                                    modelExporter.launch(model.fileName)
                                },
                                enabled = !state.busy,
                            ) { Text("Keep copy") }
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
                }
                HorizontalDivider()
            }
            if (state.developerMode) {
                Text("Advanced manual model", fontWeight = FontWeight.SemiBold)
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
                Button(
                    onClick = viewModel::downloadModel,
                    enabled = !state.busy && state.modelName.isNotBlank() && state.modelUrl.isNotBlank() &&
                        state.modelSha.matches(Regex("^[a-fA-F0-9]{64}$")),
                ) { Text(stringResource(R.string.download)) }
            }
            state.downloadProgress?.let { progress ->
                progress.fraction?.let { fraction ->
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("${progress.downloadedBytes / 1_048_576} MB", style = MaterialTheme.typography.labelSmall)
            }
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

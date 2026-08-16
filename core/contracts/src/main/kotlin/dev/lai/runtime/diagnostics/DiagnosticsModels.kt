package dev.lai.runtime.diagnostics

import kotlinx.serialization.Serializable

@Serializable
data class DiagnosticsReportV1(
    val schemaVersion: Int = 1,
    val generatedAtEpochMs: Long,
    val app: AppDiagnostics,
    val device: DeviceDiagnostics,
    val runtime: RuntimeDiagnostics,
    val models: List<ModelDiagnostics>,
    val performance: List<GenerationPerformanceDiagnostics>,
    val privacy: DiagnosticsPrivacy,
    val automation: AutomationDiagnostics = AutomationDiagnostics(),
)

@Serializable
data class AppDiagnostics(
    val versionName: String,
    val versionCode: Int,
    val productionSigned: Boolean,
    val operation: String,
    val catalogStatus: String,
)

@Serializable
data class DeviceDiagnostics(
    val manufacturer: String,
    val model: String,
    val androidSdk: Int,
    val supportedAbis: List<String>,
    val availableMemoryBytes: Long?,
    val batteryPercent: Int?,
    val charging: Boolean?,
    val thermalState: String,
    val socManufacturer: String? = null,
    val socModel: String? = null,
    val cpuCoreCount: Int? = null,
)

@Serializable
data class RuntimeDiagnostics(
    val nativeLibraryLoaded: Boolean,
    val compiledBackends: List<String>,
    val activeBackendDecision: String,
    val contextSize: Int,
    val activeModelId: String?,
    val modelLoadMs: Long?,
    val estimatedPeakBytes: Long?,
    val accessibilityConnected: Boolean,
    val shizukuState: String,
    val trimmedConversationTurns: Int,
    /**
     * Why the most recent generation attempt ended without producing tokens, if it did.
     *
     * Two field reports arrived with `performance: []` and no other clue, which made it
     * impossible to distinguish a native failure from a silent stall. This is a short, fixed
     * diagnostic reason produced by LAI itself - never model output, prompt text, or a file path.
     */
    val lastGenerationFailure: String? = null,
    /** Attempts that ended with zero tokens since app start. */
    val emptyGenerationCount: Int = 0,
)

@Serializable
data class ModelDiagnostics(
    val id: String,
    val displayName: String,
    val bytes: Long,
    val sha256: String,
    val active: Boolean,
)

@Serializable
data class GenerationPerformanceDiagnostics(
    val promptTokens: Int,
    val generatedTokens: Int,
    val promptEvaluationMs: Long,
    val timeToFirstTokenMs: Long,
    val decodeMs: Long,
    val totalMs: Long,
    val promptTokensPerSecond: Double,
    val decodeTokensPerSecond: Double,
)

@Serializable
data class AutomationDiagnostics(
    val toolProposalsEnabled: Boolean = false,
    val proposalResponsesExamined: Int = 0,
    val proposalAccepted: Int = 0,
    val proposalRejected: Int = 0,
    val proposalNotToolCall: Int = 0,
    val lastProposalOutcome: String = "NONE",
    val proposalRejectionCodes: Map<String, Int> = emptyMap(),
    val auditPersistence: String = "APP_PRIVATE_HASH_CHAIN_V1",
    val auditIntegrityValid: Boolean = true,
    val records: List<ToolAuditDiagnostics> = emptyList(),
)

@Serializable
data class ToolAuditDiagnostics(
    val toolName: String,
    val risk: String,
    val userApproved: Boolean,
    val success: Boolean?,
    val timestampEpochMs: Long,
)

@Serializable
data class DiagnosticsPrivacy(
    val localOnlyUntilUserExport: Boolean = true,
    val excludedData: List<String> = listOf(
        "prompts",
        "generated_text",
        "screenshots",
        "ocr_text",
        "accessibility_trees",
        "foreground_packages",
        "documents",
        "tool_arguments",
        "tool_outputs",
        "tool_call_fingerprints",
        "tool_audit_hashes",
        "typed_automation_text",
        "credentials",
        "network_identifiers",
    ),
)

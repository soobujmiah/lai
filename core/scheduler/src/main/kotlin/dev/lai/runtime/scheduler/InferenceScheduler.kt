package dev.lai.runtime.scheduler

import dev.lai.runtime.inference.InferenceBackend

enum class CapabilityEvidence { COMPILED, RUNTIME_PROBED, DEVICE_VALIDATED, BENCHMARKED }
enum class ThermalState { UNKNOWN, NOMINAL, LIGHT, MODERATE, SEVERE, CRITICAL }

data class BackendCapability(
    val backend: InferenceBackend,
    val supported: Boolean,
    val evidence: Set<CapabilityEvidence>,
    val estimatedPeakBytes: Long?,
    val measuredTokensPerSecond: Double? = null,
)

data class RuntimeEnvironment(
    val availableMemoryBytes: Long?,
    val batteryPercent: Int?,
    val charging: Boolean?,
    val thermalState: ThermalState,
)

data class InferenceWorkload(
    val estimatedRequiredBytes: Long?,
    val preferredBackend: InferenceBackend = InferenceBackend.AUTO,
)

data class BackendEvaluation(
    val backend: InferenceBackend,
    val eligible: Boolean,
    val rejectionReasons: List<String>,
)

data class ScheduleDecision(
    val selected: InferenceBackend,
    val evaluations: List<BackendEvaluation>,
    val reason: String,
)

class NoEligibleBackendException(val evaluations: List<BackendEvaluation>) :
    IllegalStateException("No inference backend satisfies the current evidence and device policy")

class InferenceScheduler {
    fun select(
        workload: InferenceWorkload,
        environment: RuntimeEnvironment,
        capabilities: List<BackendCapability>,
    ): ScheduleDecision {
        val evaluations = capabilities.map { capability ->
            val reasons = buildList {
                if (!capability.supported) add("Backend did not report support")
                if (CapabilityEvidence.COMPILED !in capability.evidence) add("Backend is not compile-verified")
                if (CapabilityEvidence.RUNTIME_PROBED !in capability.evidence) add("Runtime probe has not passed")
                val required = maxOfNullable(workload.estimatedRequiredBytes, capability.estimatedPeakBytes)
                environment.availableMemoryBytes?.let { available ->
                    if (required != null && required > (available * MAX_MEMORY_FRACTION).toLong()) {
                        add("Estimated peak exceeds the safe available-memory budget")
                    }
                }
                if (capability.backend in OPTIONAL_ACCELERATORS) {
                    if (CapabilityEvidence.DEVICE_VALIDATED !in capability.evidence) {
                        add("Optional accelerator has not passed physical-device validation")
                    }
                    if (environment.thermalState >= ThermalState.SEVERE) add("Thermal state blocks optional acceleration")
                    if (environment.batteryPercent != null && environment.batteryPercent < 15 && environment.charging != true) {
                        add("Low battery blocks optional acceleration")
                    }
                }
            }
            BackendEvaluation(capability.backend, reasons.isEmpty(), reasons)
        }
        val eligible = capabilities.filter { capability ->
            evaluations.first { it.backend == capability.backend }.eligible
        }
        val requested = workload.preferredBackend.takeIf { it != InferenceBackend.AUTO }
        val selected = if (requested != null) {
            eligible.firstOrNull { it.backend == requested }
        } else {
            eligible.sortedWith(
                compareByDescending<BackendCapability> { CapabilityEvidence.DEVICE_VALIDATED in it.evidence }
                    .thenByDescending { it.measuredTokensPerSecond ?: Double.NEGATIVE_INFINITY }
                    .thenBy { FALLBACK_ORDER.indexOf(it.backend).let { rank -> if (rank < 0) Int.MAX_VALUE else rank } },
            ).firstOrNull()
        } ?: throw NoEligibleBackendException(evaluations)
        return ScheduleDecision(
            selected = selected.backend,
            evaluations = evaluations,
            reason = if (selected.measuredTokensPerSecond != null) {
                "Selected using device-validated measured performance"
            } else {
                "Selected using evidence level and deterministic fallback order"
            },
        )
    }

    private fun maxOfNullable(first: Long?, second: Long?): Long? = when {
        first == null -> second
        second == null -> first
        else -> maxOf(first, second)
    }

    companion object {
        private const val MAX_MEMORY_FRACTION = 0.75
        private val OPTIONAL_ACCELERATORS = setOf(InferenceBackend.VULKAN, InferenceBackend.QNN)
        private val FALLBACK_ORDER = listOf(InferenceBackend.QNN, InferenceBackend.VULKAN, InferenceBackend.CPU)
    }
}

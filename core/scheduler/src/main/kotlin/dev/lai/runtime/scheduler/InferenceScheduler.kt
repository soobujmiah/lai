package dev.lai.runtime.scheduler

import dev.lai.runtime.inference.BackendId
import dev.lai.runtime.inference.ComputeClass

enum class CapabilityEvidence { COMPILED, RUNTIME_PROBED, DEVICE_VALIDATED, BENCHMARKED }
enum class ThermalState { UNKNOWN, NOMINAL, LIGHT, MODERATE, SEVERE, CRITICAL }

data class BackendCapability(
    val backend: BackendId,
    val computeClass: ComputeClass,
    val supported: Boolean,
    val evidence: Set<CapabilityEvidence>,
    val estimatedPeakBytes: Long?,
    val supportedModelFormats: Set<String>,
    val supportedQuantizations: Set<String> = emptySet(),
    val preference: Int = 0,
    val measuredTokensPerSecond: Double? = null,
)

data class RuntimeEnvironment(
    val availableMemoryBytes: Long?,
    val batteryPercent: Int?,
    val charging: Boolean?,
    val thermalState: ThermalState,
)

data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val socManufacturer: String?,
    val socModel: String?,
    val androidSdk: Int,
    val supportedAbis: List<String>,
    val cpuCoreCount: Int?,
    val environment: RuntimeEnvironment,
    val backends: List<BackendCapability>,
)

data class InferenceWorkload(
    val estimatedRequiredBytes: Long?,
    val modelFormat: String? = null,
    val quantization: String? = null,
    val compatibleBackends: Set<BackendId> = emptySet(),
    val backendPreference: List<BackendId> = emptyList(),
    val requiredAbis: Set<String> = emptySet(),
)

data class BackendEvaluation(
    val backend: BackendId,
    val eligible: Boolean,
    val rejectionReasons: List<String>,
)

data class ScheduleDecision(
    val selected: BackendId,
    val evaluations: List<BackendEvaluation>,
    val reason: String,
)

class NoEligibleBackendException(val evaluations: List<BackendEvaluation>) :
    IllegalStateException("No inference backend satisfies the current evidence, model, and device policy")

/**
 * Vendor-neutral policy. Backend identity, compute class, compatibility, and preference are supplied by adapters;
 * this class deliberately contains no hardware-vendor identifiers or SDK terminology.
 */
class InferenceScheduler {
    fun select(workload: InferenceWorkload, profile: DeviceProfile): ScheduleDecision =
        selectInternal(workload, profile.environment, profile.backends, profile.supportedAbis)

    fun select(
        workload: InferenceWorkload,
        environment: RuntimeEnvironment,
        capabilities: List<BackendCapability>,
    ): ScheduleDecision = selectInternal(workload, environment, capabilities, null)

    private fun selectInternal(
        workload: InferenceWorkload,
        environment: RuntimeEnvironment,
        capabilities: List<BackendCapability>,
        deviceAbis: List<String>?,
    ): ScheduleDecision {
        val evaluations = capabilities.map { capability ->
            val reasons = buildList {
                if (!capability.supported) add("Backend did not report support")
                if (CapabilityEvidence.COMPILED !in capability.evidence) add("Backend is not compile-verified")
                if (CapabilityEvidence.RUNTIME_PROBED !in capability.evidence) add("Runtime probe has not passed")
                if (workload.compatibleBackends.isNotEmpty() && capability.backend !in workload.compatibleBackends) {
                    add("Model artifact does not declare this backend as compatible")
                }
                if (
                    deviceAbis != null &&
                    workload.requiredAbis.isNotEmpty() &&
                    deviceAbis.none { deviceAbi -> workload.requiredAbis.any { it.equals(deviceAbi, ignoreCase = true) } }
                ) {
                    add("Device ABI does not satisfy the model artifact requirement")
                }
                if (!supports(workload.modelFormat, capability.supportedModelFormats)) {
                    add("Backend does not declare support for model format ${workload.modelFormat}")
                }
                if (
                    workload.quantization != null &&
                    capability.supportedQuantizations.isNotEmpty() &&
                    !supports(workload.quantization, capability.supportedQuantizations)
                ) {
                    add("Backend does not declare support for quantization ${workload.quantization}")
                }
                val required = maxOfNullable(workload.estimatedRequiredBytes, capability.estimatedPeakBytes)
                environment.availableMemoryBytes?.let { available ->
                    if (required != null && required > (available * MAX_MEMORY_FRACTION).toLong()) {
                        add("Estimated peak exceeds the safe available-memory budget")
                    }
                }
                if (capability.computeClass != ComputeClass.CPU) {
                    if (CapabilityEvidence.DEVICE_VALIDATED !in capability.evidence) {
                        add("Accelerator has not passed physical-device validation")
                    }
                    if (environment.thermalState >= ThermalState.SEVERE) add("Thermal state blocks acceleration")
                    if (environment.batteryPercent != null && environment.batteryPercent < 15 && environment.charging != true) {
                        add("Low battery blocks acceleration")
                    }
                }
            }
            BackendEvaluation(capability.backend, reasons.isEmpty(), reasons)
        }
        val eligible = capabilities.filter { capability ->
            evaluations.first { it.backend == capability.backend }.eligible
        }
        val selected = eligible.sortedWith(
            compareByDescending<BackendCapability> { CapabilityEvidence.DEVICE_VALIDATED in it.evidence }
                .thenByDescending { it.measuredTokensPerSecond ?: Double.NEGATIVE_INFINITY }
                .thenBy { modelPreferenceRank(workload.backendPreference, it.backend) }
                .thenByDescending { it.preference }
                .thenBy { it.backend.value },
        ).firstOrNull() ?: throw NoEligibleBackendException(evaluations)
        return ScheduleDecision(
            selected = selected.backend,
            evaluations = evaluations,
            reason = if (selected.measuredTokensPerSecond != null) {
                "Selected using device-validated measured performance"
            } else {
                "Selected using evidence, compatibility, and model/adapter preference"
            },
        )
    }

    private fun supports(required: String?, supported: Set<String>): Boolean {
        if (required == null) return true
        return supported.any { it.equals(required, ignoreCase = true) }
    }

    private fun modelPreferenceRank(preference: List<BackendId>, backend: BackendId): Int =
        preference.indexOf(backend).takeIf { it >= 0 } ?: Int.MAX_VALUE

    private fun maxOfNullable(first: Long?, second: Long?): Long? = when {
        first == null -> second
        second == null -> first
        else -> maxOf(first, second)
    }

    private companion object {
        const val MAX_MEMORY_FRACTION = 0.75
    }
}

package dev.lai.runtime.scheduler

import dev.lai.runtime.inference.BackendId
import dev.lai.runtime.inference.ComputeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceSchedulerTest {
    private val scheduler = InferenceScheduler()
    private val ready = setOf(CapabilityEvidence.COMPILED, CapabilityEvidence.RUNTIME_PROBED)
    private val cpu = BackendId("test-cpu")
    private val accelerator = BackendId("test-accelerator")

    @Test
    fun `validated accelerator wins when device is healthy`() {
        val decision = scheduler.select(
            InferenceWorkload(estimatedRequiredBytes = 1_000, modelFormat = "gguf"),
            RuntimeEnvironment(10_000, 80, false, ThermalState.NOMINAL),
            listOf(
                capability(cpu, ComputeClass.CPU, ready, measured = 8.0),
                capability(
                    accelerator,
                    ComputeClass.GPU,
                    ready + CapabilityEvidence.DEVICE_VALIDATED,
                    measured = 20.0,
                ),
            ),
        )
        assertEquals(accelerator, decision.selected)
    }

    @Test
    fun `severe thermal state falls back to CPU without knowing a vendor`() {
        val decision = scheduler.select(
            InferenceWorkload(estimatedRequiredBytes = 1_000, modelFormat = "gguf"),
            RuntimeEnvironment(10_000, 80, false, ThermalState.SEVERE),
            listOf(
                capability(cpu, ComputeClass.CPU, ready),
                capability(
                    accelerator,
                    ComputeClass.NPU,
                    ready + CapabilityEvidence.DEVICE_VALIDATED,
                ),
            ),
        )
        assertEquals(cpu, decision.selected)
        assertTrue(decision.evaluations.first { it.backend == accelerator }.rejectionReasons.isNotEmpty())
    }

    @Test
    fun `model format incompatibility rejects an otherwise ready backend`() {
        val decision = scheduler.select(
            InferenceWorkload(estimatedRequiredBytes = 1_000, modelFormat = "gguf"),
            RuntimeEnvironment(10_000, 80, true, ThermalState.NOMINAL),
            listOf(
                capability(cpu, ComputeClass.CPU, ready),
                capability(
                    BackendId("context-binary-npu"),
                    ComputeClass.NPU,
                    ready + CapabilityEvidence.DEVICE_VALIDATED,
                    formats = setOf("qnn-context"),
                ),
            ),
        )
        assertEquals(cpu, decision.selected)
        assertTrue(
            decision.evaluations
                .first { it.backend.value == "context-binary-npu" }
                .rejectionReasons
                .any { it.contains("model format") },
        )
    }

    @Test
    fun `device profile overload preserves generic hardware facts`() {
        val profile = DeviceProfile(
            manufacturer = "vendor",
            model = "device",
            socManufacturer = null,
            socModel = null,
            androidSdk = 35,
            supportedAbis = listOf("arm64-v8a"),
            cpuCoreCount = 8,
            environment = RuntimeEnvironment(10_000, 80, true, ThermalState.NOMINAL),
            backends = listOf(capability(cpu, ComputeClass.CPU, ready)),
        )
        val decision = scheduler.select(
            InferenceWorkload(
                estimatedRequiredBytes = 1_000,
                modelFormat = "GGUF",
                compatibleBackends = setOf(cpu),
                backendPreference = listOf(cpu),
                requiredAbis = setOf("arm64-v8a"),
            ),
            profile,
        )
        assertEquals(cpu, decision.selected)
        assertEquals(8, profile.cpuCoreCount)
    }

    @Test
    fun `device profile rejects incompatible ABI`() {
        val profile = DeviceProfile(
            manufacturer = "vendor",
            model = "device",
            socManufacturer = null,
            socModel = null,
            androidSdk = 35,
            supportedAbis = listOf("x86_64"),
            cpuCoreCount = 4,
            environment = RuntimeEnvironment(10_000, 80, true, ThermalState.NOMINAL),
            backends = listOf(capability(cpu, ComputeClass.CPU, ready)),
        )
        val error = runCatching {
            scheduler.select(
                InferenceWorkload(1_000, modelFormat = "gguf", requiredAbis = setOf("arm64-v8a")),
                profile,
            )
        }.exceptionOrNull() as NoEligibleBackendException
        assertTrue(error.evaluations.single().rejectionReasons.any { it.contains("ABI") })
    }

    @Test
    fun `declared quantization incompatibility is explicit`() {
        val incompatible = BackendCapability(
            backend = cpu,
            computeClass = ComputeClass.CPU,
            supported = true,
            evidence = ready,
            estimatedPeakBytes = 1_000,
            supportedModelFormats = setOf("gguf"),
            supportedQuantizations = setOf("Q8_0"),
        )
        val error = runCatching {
            scheduler.select(
                InferenceWorkload(1_000, modelFormat = "gguf", quantization = "Q4_K_M"),
                RuntimeEnvironment(10_000, 80, true, ThermalState.NOMINAL),
                listOf(incompatible),
            )
        }.exceptionOrNull() as NoEligibleBackendException
        assertTrue(error.evaluations.single().rejectionReasons.any { it.contains("quantization") })
    }

    @Test(expected = NoEligibleBackendException::class)
    fun `runtime probe evidence is mandatory`() {
        scheduler.select(
            InferenceWorkload(estimatedRequiredBytes = 1_000, modelFormat = "gguf"),
            RuntimeEnvironment(10_000, 80, true, ThermalState.NOMINAL),
            listOf(capability(cpu, ComputeClass.CPU, setOf(CapabilityEvidence.COMPILED))),
        )
    }

    private fun capability(
        id: BackendId,
        computeClass: ComputeClass,
        evidence: Set<CapabilityEvidence>,
        measured: Double? = null,
        formats: Set<String> = setOf("gguf"),
    ) = BackendCapability(
        backend = id,
        computeClass = computeClass,
        supported = true,
        evidence = evidence,
        estimatedPeakBytes = 1_000,
        supportedModelFormats = formats,
        measuredTokensPerSecond = measured,
    )
}

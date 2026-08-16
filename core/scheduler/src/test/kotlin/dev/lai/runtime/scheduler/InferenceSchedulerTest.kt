package dev.lai.runtime.scheduler

import dev.lai.runtime.inference.InferenceBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceSchedulerTest {
    private val scheduler = InferenceScheduler()
    private val ready = setOf(CapabilityEvidence.COMPILED, CapabilityEvidence.RUNTIME_PROBED)

    @Test
    fun `validated accelerator wins when device is healthy`() {
        val decision = scheduler.select(
            InferenceWorkload(1_000),
            RuntimeEnvironment(10_000, 80, false, ThermalState.NOMINAL),
            listOf(
                BackendCapability(InferenceBackend.CPU, true, ready, 1_000, 8.0),
                BackendCapability(
                    InferenceBackend.VULKAN,
                    true,
                    ready + CapabilityEvidence.DEVICE_VALIDATED,
                    1_000,
                    20.0,
                ),
            ),
        )
        assertEquals(InferenceBackend.VULKAN, decision.selected)
    }

    @Test
    fun `severe thermal state falls back to CPU`() {
        val decision = scheduler.select(
            InferenceWorkload(1_000),
            RuntimeEnvironment(10_000, 80, false, ThermalState.SEVERE),
            listOf(
                BackendCapability(InferenceBackend.CPU, true, ready, 1_000),
                BackendCapability(InferenceBackend.QNN, true, ready, 1_000),
            ),
        )
        assertEquals(InferenceBackend.CPU, decision.selected)
        assertTrue(decision.evaluations.first { it.backend == InferenceBackend.QNN }.rejectionReasons.isNotEmpty())
    }

    @Test(expected = NoEligibleBackendException::class)
    fun `runtime probe evidence is mandatory`() {
        scheduler.select(
            InferenceWorkload(1_000),
            RuntimeEnvironment(10_000, 80, true, ThermalState.NOMINAL),
            listOf(BackendCapability(InferenceBackend.CPU, true, setOf(CapabilityEvidence.COMPILED), 1_000)),
        )
    }
}

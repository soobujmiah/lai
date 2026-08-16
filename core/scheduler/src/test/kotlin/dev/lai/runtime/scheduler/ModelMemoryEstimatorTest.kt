package dev.lai.runtime.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelMemoryEstimatorTest {
    private val estimator = ModelMemoryEstimator()

    @Test
    fun `estimate reserves weights KV and workspace`() {
        val result = estimator.estimate(1_117_320_736, 4096)
        assertEquals(1_117_320_736, result.mappedWeightsBytes)
        assertEquals(4096L * 128L * 1024L, result.kvCacheReserveBytes)
        assertTrue(result.computeWorkspaceBytes >= 256L * 1024L * 1024L)
        assertEquals(
            result.mappedWeightsBytes + result.kvCacheReserveBytes + result.computeWorkspaceBytes,
            result.estimatedPeakBytes,
        )
        assertEquals(EstimateConfidence.CONSERVATIVE_GENERIC, result.confidence)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid context is rejected`() {
        estimator.estimate(100, 1)
    }
}

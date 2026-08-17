package dev.lai.runtime.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalGovernorPolicyTest {

    private val policy = ThermalGovernorPolicy()

    @Test
    fun `nominal light and unknown run the full baseline`() {
        for (state in listOf(ThermalState.NOMINAL, ThermalState.LIGHT, ThermalState.UNKNOWN)) {
            val decision = policy.decide(state, previous = null, baselineThreads = 4)
            assertEquals(4, decision.decodeThreads)
            assertTrue(decision.admitNewGeneration)
            assertNull(decision.reason)
        }
    }

    @Test
    fun `moderate drops one thread but keeps admitting`() {
        val decision = policy.decide(ThermalState.MODERATE, previous = null, baselineThreads = 4)
        assertEquals(3, decision.decodeThreads)
        assertTrue(decision.admitNewGeneration)
        assertEquals("Reduced CPU threads to limit heat", decision.reason)
    }

    @Test
    fun `moderate never goes below two threads`() {
        assertEquals(2, policy.decide(ThermalState.MODERATE, null, baselineThreads = 2).decodeThreads)
    }

    @Test
    fun `severe halves work and blocks new generations`() {
        val decision = policy.decide(ThermalState.SEVERE, previous = null, baselineThreads = 4)
        assertEquals(2, decision.decodeThreads)
        assertFalse(decision.admitNewGeneration)
    }

    @Test
    fun `critical cuts to two threads minimum`() {
        val decision = policy.decide(ThermalState.CRITICAL, previous = null, baselineThreads = 4)
        assertEquals(2, decision.decodeThreads)
        assertFalse(decision.admitNewGeneration)
    }

    @Test
    fun `threads fall immediately when heat rises`() {
        val hot = policy.decide(ThermalState.SEVERE, previous = null, baselineThreads = 4)
        val hotter = policy.decide(ThermalState.CRITICAL, previous = hot, baselineThreads = 4)
        assertEquals(2, hotter.decodeThreads)
    }

    @Test
    fun `recovery is sticky - threads only rise again at fully nominal`() {
        val reduced = policy.decide(ThermalState.SEVERE, previous = null, baselineThreads = 4)
        // Cooling to LIGHT (or MODERATE) must NOT restore threads: that is the hysteresis band.
        val stillReducedLight = policy.decide(ThermalState.LIGHT, previous = reduced, baselineThreads = 4)
        assertEquals(reduced.decodeThreads, stillReducedLight.decodeThreads)
        // But admission recovers immediately - the user can send again while threads stay low.
        assertTrue(stillReducedLight.admitNewGeneration)
        val restored = policy.decide(ThermalState.NOMINAL, previous = stillReducedLight, baselineThreads = 4)
        assertEquals(4, restored.decodeThreads)
        assertNull(restored.reason)
    }

    @Test
    fun `moderate to light keeps the reduced thread count`() {
        val reduced = policy.decide(ThermalState.MODERATE, previous = null, baselineThreads = 4)
        val light = policy.decide(ThermalState.LIGHT, previous = reduced, baselineThreads = 4)
        assertEquals(3, light.decodeThreads)
    }

    @Test
    fun `baseline is clamped to a sane range`() {
        assertEquals(8, policy.decide(ThermalState.NOMINAL, null, baselineThreads = 64).decodeThreads)
        assertEquals(1, policy.decide(ThermalState.NOMINAL, null, baselineThreads = 0).decodeThreads)
    }
}

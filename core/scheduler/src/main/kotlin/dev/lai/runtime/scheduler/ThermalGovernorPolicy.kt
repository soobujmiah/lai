package dev.lai.runtime.scheduler

/**
 * Closed-loop thermal governor decisions.
 *
 * Prior behaviour was admission-only: generation was refused at SEVERE+, but an in-flight decode
 * kept burning all its threads while the device heated (field report: hot within one long
 * reply). This policy adds the missing loop: it maps the live thermal state to a decode thread
 * budget, and recovery is deliberately sticky (hysteresis) so the governor cannot flap between
 * two thread counts at a state boundary.
 *
 * Pure JVM by design (core:scheduler) — the Android thermal callback and the native thread
 * control are injected at the edges.
 */
class ThermalGovernorPolicy {

    data class Decision(
        val decodeThreads: Int,
        val admitNewGeneration: Boolean,
        /** LAI-authored short reason shown in the UI when the governor intervenes; null = normal. */
        val reason: String?,
    )

    /**
     * @param state current Android thermal status.
     * @param previous the decision currently in force, or null on first evaluation.
     * @param baselineThreads the unthrottled decode thread count (half the cores, 2..4 — must
     *   match the native context configuration).
     */
    fun decide(state: ThermalState, previous: Decision?, baselineThreads: Int): Decision {
        val baseline = baselineThreads.coerceIn(1, 8)
        val target = when (state) {
            ThermalState.NOMINAL, ThermalState.LIGHT, ThermalState.UNKNOWN ->
                Decision(baseline, admitNewGeneration = true, reason = null)
            ThermalState.MODERATE -> Decision(
                decodeThreads = (baseline - 1).coerceAtLeast(2),
                admitNewGeneration = true,
                reason = "Reduced CPU threads to limit heat",
            )
            ThermalState.SEVERE -> Decision(
                decodeThreads = 2.coerceAtMost(baseline),
                admitNewGeneration = false,
                reason = "Device is hot: replies are slowed and new ones wait",
            )
            ThermalState.CRITICAL -> Decision(
                decodeThreads = 2.coerceAtMost(baseline),
                admitNewGeneration = false,
                reason = "Device is very hot: cooling takes priority",
            )
        }
        // Hysteresis: thread count may always fall immediately, but may only RISE again once the
        // device is fully NOMINAL. Without this, hovering at a status boundary (e.g.
        // LIGHT↔MODERATE) would toggle threads every callback and produce visible speed flapping.
        if (previous != null && target.decodeThreads > previous.decodeThreads && state != ThermalState.NOMINAL) {
            return previous.copy(admitNewGeneration = target.admitNewGeneration)
        }
        return target
    }
}

package dev.lai.runtime.scheduler

data class ModelMemoryEstimate(
    val mappedWeightsBytes: Long,
    val kvCacheReserveBytes: Long,
    val computeWorkspaceBytes: Long,
    val estimatedPeakBytes: Long,
    val confidence: EstimateConfidence,
)

enum class EstimateConfidence { CONSERVATIVE_GENERIC, MODEL_METADATA }

/**
 * Conservative preflight used before native metadata is available. The runtime
 * will replace this with architecture-derived KV/workspace values after probing.
 */
class ModelMemoryEstimator {
    fun estimate(modelFileBytes: Long, contextTokens: Int): ModelMemoryEstimate {
        require(modelFileBytes > 0) { "Model file size must be positive" }
        require(contextTokens in 256..131_072) { "Context size is outside the supported range" }
        val kvReserve = saturatedMultiply(contextTokens.toLong(), KV_BYTES_PER_TOKEN)
        val workspace = maxOf(MIN_WORKSPACE_BYTES, saturatedMultiply(modelFileBytes, WORKSPACE_PERCENT) / 100)
        val peak = saturatedAdd(saturatedAdd(modelFileBytes, kvReserve), workspace)
        return ModelMemoryEstimate(
            mappedWeightsBytes = modelFileBytes,
            kvCacheReserveBytes = kvReserve,
            computeWorkspaceBytes = workspace,
            estimatedPeakBytes = peak,
            confidence = EstimateConfidence.CONSERVATIVE_GENERIC,
        )
    }

    private fun saturatedMultiply(left: Long, right: Long): Long =
        if (left > Long.MAX_VALUE / right) Long.MAX_VALUE else left * right

    private fun saturatedAdd(left: Long, right: Long): Long =
        if (left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    companion object {
        private const val KV_BYTES_PER_TOKEN = 128L * 1024L
        private const val MIN_WORKSPACE_BYTES = 256L * 1024L * 1024L
        private const val WORKSPACE_PERCENT = 15L
    }
}

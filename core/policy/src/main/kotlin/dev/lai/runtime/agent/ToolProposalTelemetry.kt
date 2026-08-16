package dev.lai.runtime.agent

/** Session-only, content-free parser outcomes suitable for Developer Mode and explicit diagnostics export. */
data class ToolProposalCounters(
    val responsesExamined: Int = 0,
    val accepted: Int = 0,
    val rejected: Int = 0,
    val notToolCall: Int = 0,
    val lastOutcome: String = "NONE",
    val rejectionCodes: Map<String, Int> = emptyMap(),
) {
    fun record(result: ToolCallParseResult): ToolProposalCounters = when (result) {
        ToolCallParseResult.NotToolCall -> copy(
            responsesExamined = increment(responsesExamined),
            notToolCall = increment(notToolCall),
            lastOutcome = "NOT_TOOL_CALL",
        )
        is ToolCallParseResult.Accepted -> copy(
            responsesExamined = increment(responsesExamined),
            accepted = increment(accepted),
            lastOutcome = "ACCEPTED",
        )
        is ToolCallParseResult.Rejected -> copy(
            responsesExamined = increment(responsesExamined),
            rejected = increment(rejected),
            lastOutcome = "REJECTED_${result.code}",
            rejectionCodes = rejectionCodes + (
                result.code to increment(rejectionCodes[result.code] ?: 0)
            ),
        )
    }

    private fun increment(value: Int): Int = if (value == Int.MAX_VALUE) value else value + 1
}

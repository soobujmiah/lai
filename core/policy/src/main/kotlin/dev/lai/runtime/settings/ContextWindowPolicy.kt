package dev.lai.runtime.settings

import dev.lai.runtime.inference.ConversationMessage
import dev.lai.runtime.inference.ConversationRole

/**
 * Rolling context window: bounds how many completed turns of history accompany a request.
 *
 * `keepLastTurns` has been typed, validated ([SettingsPolicy], range 1..64), and user-editable in
 * the quick-settings sheet since Phase 2A — but was never applied. This makes it real.
 *
 * Semantics:
 * - A **turn** starts at each USER message. The trailing USER message is the in-flight request,
 *   not a completed turn, and is always kept.
 * - The window keeps the last [keepLastTurns] completed turns (each with its replies) plus the
 *   in-flight request; older messages are dropped **from the front**, so what remains is always
 *   a contiguous tail of the conversation starting at a USER message.
 * - SYSTEM messages are the caller's concern (the tool instruction is prepended after windowing);
 *   this policy only ever sees USER/ASSISTANT history.
 *
 * Interaction with native KV-prefix reuse: while the conversation is at or under the window, the
 * prompt prefix is stable and reuse is maximal. Once the window starts sliding, each request
 * changes the prefix and pays a full re-prefill of the kept window — a *bounded* cost, which is
 * exactly the point of the setting. Pure JVM by design (core:policy).
 */
object ContextWindowPolicy {

    data class WindowedConversation(
        val messages: List<ConversationMessage>,
        /** Completed turns dropped by the window (not token-overflow trims — those are separate). */
        val droppedTurns: Int,
    )

    fun applyTurnWindow(history: List<ConversationMessage>, keepLastTurns: Int): WindowedConversation {
        val keep = keepLastTurns.coerceAtLeast(1)
        val turnStarts = history.indices.filter { history[it].role == ConversationRole.USER }
        // No user message at all: nothing that can be counted as a turn; leave untouched.
        if (turnStarts.isEmpty()) return WindowedConversation(history, 0)
        // The final USER message opens the in-flight turn; everything before its predecessors
        // are completed turns.
        val completedStarts = turnStarts.dropLast(1)
        if (completedStarts.size <= keep) return WindowedConversation(history, 0)
        val firstKeptIndex = completedStarts[completedStarts.size - keep]
        return WindowedConversation(
            messages = history.subList(firstKeptIndex, history.size).toList(),
            droppedTurns = completedStarts.size - keep,
        )
    }
}

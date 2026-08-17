package dev.lai.runtime.settings

import dev.lai.runtime.inference.ConversationMessage
import dev.lai.runtime.inference.ConversationRole
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextWindowPolicyTest {

    private fun user(text: String) = ConversationMessage(ConversationRole.USER, text)
    private fun assistant(text: String) = ConversationMessage(ConversationRole.ASSISTANT, text)

    @Test
    fun `conversation within the window is untouched`() {
        val history = listOf(user("u1"), assistant("a1"), user("u2"), assistant("a2"), user("u3"))
        val result = ContextWindowPolicy.applyTurnWindow(history, keepLastTurns = 2)
        assertEquals(history, result.messages)
        assertEquals(0, result.droppedTurns)
    }

    @Test
    fun `oldest completed turns are dropped from the front`() {
        val history = listOf(
            user("u1"), assistant("a1"),
            user("u2"), assistant("a2"),
            user("u3"), assistant("a3"),
            user("u4"),
        )
        val result = ContextWindowPolicy.applyTurnWindow(history, keepLastTurns = 2)
        assertEquals(
            listOf(user("u2"), assistant("a2"), user("u3"), assistant("a3"), user("u4")),
            result.messages,
        )
        assertEquals(1, result.droppedTurns)
        assertEquals(ConversationRole.USER, result.messages.first().role)
    }

    @Test
    fun `keepLastTurns of one keeps only the previous exchange and the request`() {
        val history = listOf(
            user("u1"), assistant("a1"),
            user("u2"), assistant("a2"),
            user("u3"), assistant("a3"),
            user("u4"),
        )
        val result = ContextWindowPolicy.applyTurnWindow(history, keepLastTurns = 1)
        assertEquals(listOf(user("u3"), assistant("a3"), user("u4")), result.messages)
        assertEquals(2, result.droppedTurns)
    }

    @Test
    fun `bangla content survives the window intact`() {
        val history = listOf(
            user("প্রথম প্রশ্ন"), assistant("প্রথম উত্তর"),
            user("দ্বিতীয় প্রশ্ন"), assistant("দ্বিতীয় উত্তর"),
            user("বর্তমান প্রশ্ন"),
        )
        val result = ContextWindowPolicy.applyTurnWindow(history, keepLastTurns = 1)
        assertEquals(listOf(user("দ্বিতীয় প্রশ্ন"), assistant("দ্বিতীয় উত্তর"), user("বর্তমান প্রশ্ন")), result.messages)
        assertEquals(1, result.droppedTurns)
    }

    @Test
    fun `first message and assistant-only history are left untouched`() {
        val single = listOf(user("first ever message"))
        assertEquals(single, ContextWindowPolicy.applyTurnWindow(single, 1).messages)
        assertEquals(0, ContextWindowPolicy.applyTurnWindow(single, 1).droppedTurns)

        val assistantOnly = listOf(assistant("dangling"))
        assertEquals(assistantOnly, ContextWindowPolicy.applyTurnWindow(assistantOnly, 1).messages)
    }

    @Test
    fun `invalid keepLastTurns is clamped to one instead of erasing history`() {
        val history = listOf(user("u1"), assistant("a1"), user("u2"), assistant("a2"), user("u3"))
        val result = ContextWindowPolicy.applyTurnWindow(history, keepLastTurns = 0)
        assertEquals(listOf(user("u2"), assistant("a2"), user("u3")), result.messages)
        assertEquals(1, result.droppedTurns)
    }
}

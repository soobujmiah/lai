package dev.lai.runtime.history

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHistoryRepositoryTest {

    private val directory: File = Files.createTempDirectory("lai-chats").toFile()
    private val repository = ChatHistoryRepository(directory)

    @After
    fun cleanup() {
        directory.deleteRecursively()
    }

    private fun session(
        id: String,
        updatedAt: Long,
        messages: List<StoredChatMessage> = listOf(
            StoredChatMessage(true, "প্রশ্ন", 1L),
            StoredChatMessage(false, "উত্তর", 2L),
        ),
    ) = StoredChatSession(
        id = id,
        title = messages.firstOrNull { it.fromUser }?.text ?: "Chat",
        createdAtEpochMs = 1L,
        updatedAtEpochMs = updatedAt,
        messages = messages,
    )

    @Test
    fun `save then load round-trips bangla content exactly`() = runBlocking {
        val saved = session("aaaa1111", updatedAt = 10L)
        assertTrue(repository.save(saved).isSuccess)
        val loaded = repository.load("aaaa1111")
        assertEquals(saved, loaded)
        assertEquals("প্রশ্ন", loaded?.messages?.first()?.text)
    }

    @Test
    fun `list returns summaries newest first without loading full content`() = runBlocking {
        repository.save(session("aaaa1111", updatedAt = 10L))
        repository.save(session("bbbb2222", updatedAt = 30L))
        repository.save(session("cccc3333", updatedAt = 20L))
        val summaries = repository.list()
        assertEquals(listOf("bbbb2222", "cccc3333", "aaaa1111"), summaries.map { it.id })
        assertEquals(2, summaries.first().messageCount)
    }

    @Test
    fun `delete removes exactly one session`() = runBlocking {
        repository.save(session("aaaa1111", updatedAt = 10L))
        repository.save(session("bbbb2222", updatedAt = 20L))
        assertTrue(repository.delete("aaaa1111"))
        assertNull(repository.load("aaaa1111"))
        assertEquals(1, repository.list().size)
    }

    @Test
    fun `corrupt files are skipped instead of failing the feature`() = runBlocking {
        repository.save(session("aaaa1111", updatedAt = 10L))
        File(directory, "chat-corrupt1.json").writeText("{not json")
        val summaries = repository.list()
        assertEquals(listOf("aaaa1111"), summaries.map { it.id })
    }

    @Test
    fun `invalid ids are rejected for save load and delete`() = runBlocking {
        assertTrue(repository.save(session("../escape", updatedAt = 1L)).isFailure)
        assertNull(repository.load("../escape"))
        assertFalse(repository.delete("../escape"))
        assertNull(repository.load("UPPER-NOT-ALLOWED"))
    }

    @Test
    fun `messages beyond the per-session bound drop oldest first`() = runBlocking {
        val many = (1..600).map { StoredChatMessage(it % 2 == 0, "m$it", it.toLong()) }
        repository.save(session("aaaa1111", updatedAt = 5L, messages = many))
        val loaded = repository.load("aaaa1111")
        assertEquals(512, loaded?.messages?.size)
        assertEquals("m600", loaded?.messages?.last()?.text)
        assertEquals("m89", loaded?.messages?.first()?.text)
    }

    @Test
    fun `session count is pruned to the newest hundred`() = runBlocking {
        repeat(105) { index ->
            repository.save(session("aaaa${"%04d".format(index)}", updatedAt = index.toLong()))
        }
        val summaries = repository.list()
        assertEquals(100, summaries.size)
        // The five oldest are gone.
        assertTrue(summaries.none { it.updatedAtEpochMs < 5L })
    }
}

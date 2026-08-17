package dev.lai.runtime.history

import kotlinx.serialization.Serializable

/**
 * On-device chat history contracts. Content-bearing by design — and therefore governed by the
 * strictest storage rules in the app: sessions live only in app-private no-backup storage, are
 * never exported by diagnostics (which exclude prompts and generated text by schema), and never
 * cross a network boundary. Pure JVM types; the Android storage authority is `platform:history`.
 */
@Serializable
data class StoredChatMessage(
    val fromUser: Boolean,
    val text: String,
    val atEpochMs: Long,
)

@Serializable
data class StoredChatSession(
    val schemaVersion: Int = 1,
    val id: String,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val messages: List<StoredChatMessage>,
)

/** Listing row: everything needed to render the history sheet without loading full content. */
data class ChatSessionSummary(
    val id: String,
    val title: String,
    val updatedAtEpochMs: Long,
    val messageCount: Int,
)

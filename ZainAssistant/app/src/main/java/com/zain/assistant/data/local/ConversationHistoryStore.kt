package com.zain.assistant.data.local

import android.content.Context
import com.zain.assistant.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists conversation history as a JSON file in app-private storage.
 * A lightweight alternative to Room that avoids annotation-processing build complexity
 * while still giving durable, structured persistence across app restarts.
 */
class ConversationHistoryStore(context: Context) {

    private val file = File(context.filesDir, "conversation_history.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun load(): List<Message> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            json.decodeFromString<List<Message>>(file.readText())
        }.getOrDefault(emptyList())
    }

    suspend fun save(messages: List<Message>) = withContext(Dispatchers.IO) {
        val trimmed = if (messages.size > MAX_HISTORY) messages.takeLast(MAX_HISTORY) else messages
        file.writeText(json.encodeToString(trimmed))
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete()
    }

    companion object {
        private const val MAX_HISTORY = 500
    }
}

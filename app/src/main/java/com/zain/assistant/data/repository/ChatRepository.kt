package com.zain.assistant.data.repository

import com.zain.assistant.data.local.ConversationHistoryStore
import com.zain.assistant.data.remote.ApiClient
import com.zain.assistant.data.remote.ChatCompletionRequest
import com.zain.assistant.data.remote.ChatMessageDto
import com.zain.assistant.domain.model.Message
import com.zain.assistant.domain.model.Sender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

sealed class ChatResult {
    data class Success(val reply: String) : ChatResult()
    data class Failure(val message: String) : ChatResult()
}

/**
 * Owns conversation state (in-memory + persisted) and talks to the configured AI provider.
 * The system prompt keeps replies short and speakable, since output is read aloud via TTS.
 */
class ChatRepository(
    private val historyStore: ConversationHistoryStore,
    private val settingsRepository: SettingsRepository
) {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    suspend fun loadHistory() {
        _messages.value = historyStore.load()
    }

    suspend fun clearHistory() {
        _messages.value = emptyList()
        historyStore.clear()
    }

    private suspend fun appendMessage(message: Message) {
        _messages.value = _messages.value + message
        historyStore.save(_messages.value)
    }

    suspend fun sendUserMessage(text: String): ChatResult {
        appendMessage(Message(UUID.randomUUID().toString(), Sender.USER, text, System.currentTimeMillis()))
        return sendToProvider(text)
    }

    private suspend fun sendToProvider(text: String): ChatResult {
        return try {
            val apiKey = settingsRepository.apiKey.first()
            val baseUrl = settingsRepository.apiBaseUrl.first()
            val model = settingsRepository.apiModel.first()

            if (apiKey.isBlank()) {
                val fallback = "I don't have an API key configured yet. Please add one in Settings so I can have full conversations."
                appendMessage(Message(UUID.randomUUID().toString(), Sender.ASSISTANT, fallback, System.currentTimeMillis()))
                return ChatResult.Failure(fallback)
            }

            val service = ApiClient.create(baseUrl)
            val history = _messages.value.takeLast(20).map {
                ChatMessageDto(
                    role = if (it.sender == Sender.USER) "user" else "assistant",
                    content = it.text
                )
            }
            val systemPrompt = ChatMessageDto(
                role = "system",
                content = "You are Zain, a helpful, warm, concise voice assistant running on the user's phone. " +
                    "Keep replies short (1-3 sentences) and conversational since they will be read aloud by text-to-speech."
            )

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(systemPrompt) + history
            )

            val response = service.chatCompletion("Bearer $apiKey", request)
            if (response.isSuccessful) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content?.trim()
                    ?: "I didn't get a response back — please try again."
                appendMessage(Message(UUID.randomUUID().toString(), Sender.ASSISTANT, reply, System.currentTimeMillis()))
                ChatResult.Success(reply)
            } else {
                val errorMsg = "The AI service returned an error (code ${response.code()}). Check your API key and endpoint in Settings."
                appendMessage(Message(UUID.randomUUID().toString(), Sender.ASSISTANT, errorMsg, System.currentTimeMillis()))
                ChatResult.Failure(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "I couldn't reach the AI service. Check your internet connection and try again."
            appendMessage(Message(UUID.randomUUID().toString(), Sender.ASSISTANT, errorMsg, System.currentTimeMillis()))
            ChatResult.Failure(errorMsg)
        }
    }

    /** Used for offline command acknowledgements — added directly to history without an API call. */
    suspend fun appendAssistantReply(text: String) {
        appendMessage(Message(UUID.randomUUID().toString(), Sender.ASSISTANT, text, System.currentTimeMillis()))
    }

    suspend fun appendUserOnly(text: String) {
        appendMessage(Message(UUID.randomUUID().toString(), Sender.USER, text, System.currentTimeMillis()))
    }
}

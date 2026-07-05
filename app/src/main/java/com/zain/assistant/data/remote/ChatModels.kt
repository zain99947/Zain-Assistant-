package com.zain.assistant.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 500
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessageDto,
    val finish_reason: String? = null
)

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorBody? = null
)

@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val type: String? = null
)

package com.zain.assistant.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Sender { USER, ASSISTANT }

@Serializable
data class Message(
    val id: String,
    val sender: Sender,
    val text: String,
    val timestamp: Long
)

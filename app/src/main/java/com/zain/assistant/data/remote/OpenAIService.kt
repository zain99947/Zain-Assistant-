package com.zain.assistant.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface targeting the OpenAI-compatible /v1/chat/completions endpoint.
 * Because this is the de-facto standard chat completion shape, pointing apiBaseUrl at
 * any compatible provider (OpenAI, Azure OpenAI proxy, OpenRouter, Groq, local Ollama
 * with an OpenAI-compat shim, etc.) works without code changes — only Settings need updating.
 */
interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}

package com.example.mindweaverstudio.data.repository.gemini

import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.model.chat.ChatRequest
import com.example.mindweaverstudio.data.network.GeminiApiClient
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository

class GeminiRepositoryImpl(
    private val apiClient: GeminiApiClient
) : NeuralNetworkRepository {

    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): Result<String> {
        val request = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens
        )

        return apiClient.createChatCompletion(request).fold(
            onSuccess = { response ->
                response.error?.let { error ->
                    return Result.failure(Exception("Gemini API Error: ${error.message}"))
                }
                
                val content = response.choices?.firstOrNull()?.message?.content
                    ?: return Result.failure(Exception("No response content available"))
                
                Result.success(content)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
package com.example.mindweaverstudio.data.repository.deepseek

import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.model.chat.ChatRequest
import com.example.mindweaverstudio.data.network.DeepSeekApiClient
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository

class DeepSeekRepositoryImpl(
    private val apiClient: DeepSeekApiClient
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
                // Check for API error first
                response.error?.let { error ->
                    return Result.failure(Exception("DeepSeek API Error: ${error.message}"))
                }
                
                // Extract content from successful response
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
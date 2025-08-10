package com.example.mindweaverstudio.data.repository.chatgpt

import com.example.mindweaverstudio.data.model.deepseek.ChatMessage
import com.example.mindweaverstudio.data.model.deepseek.ChatRequest
import com.example.mindweaverstudio.data.network.ChatGPTApiClient
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository

class ChatGPTRepositoryImpl(
    private val apiClient: ChatGPTApiClient
) : NeuralNetworkRepository {

    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): Result<String> {
        val request = ChatRequest(
            model = "gpt-3.5-turbo",
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens
        )

        return apiClient.createChatCompletion(request).fold(
            onSuccess = { response ->
                response.error?.let { error ->
                    return Result.failure(Exception("ChatGPT API Error: ${error.message}"))
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
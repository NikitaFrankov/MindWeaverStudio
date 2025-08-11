package com.example.mindweaverstudio.data.repository

import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.model.chat.StructuredOutput

interface NeuralNetworkRepository {
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        model: String = "deepseek-chat",
        temperature: Double = 0.7,
        maxTokens: Int = 1000
    ): Result<StructuredOutput>
}
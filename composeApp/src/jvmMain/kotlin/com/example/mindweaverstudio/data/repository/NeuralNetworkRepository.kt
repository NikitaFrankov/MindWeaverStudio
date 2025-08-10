package com.example.mindweaverstudio.data.repository

import com.example.mindweaverstudio.data.model.deepseek.ChatMessage

interface NeuralNetworkRepository {
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        model: String = "deepseek-chat",
        temperature: Double = 0.7,
        maxTokens: Int = 1000
    ): Result<String>
}
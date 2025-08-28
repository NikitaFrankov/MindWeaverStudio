package com.example.mindweaverstudio.data.ai.aiClients

import com.example.mindweaverstudio.data.models.ai.AiResponse
import com.example.mindweaverstudio.data.models.chat.ChatMessage

interface AiClient {
    suspend fun createChatCompletion(
        messages: List<ChatMessage>,
        temperature: Double = 0.7,
        maxTokens: Int = 1000,
    ): Result<AiResponse>
}
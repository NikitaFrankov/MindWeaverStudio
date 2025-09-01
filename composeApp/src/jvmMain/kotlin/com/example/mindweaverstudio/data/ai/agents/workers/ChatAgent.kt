package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.successPipelineResult
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_USER

class ChatAgent(
    private val aiClient: AiClient,
) : Agent {
    override val name = CHAT_AGENT
    override val description: String = "Агент для обычного общения"

    override suspend fun run(input: String): PipelineResult {
        val messages = listOf(ChatMessage(role = ROLE_USER, content = input))

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.4,
            maxTokens = 2000,
        )
        return result.fold(
            onSuccess = { response ->
                successPipelineResult(message = response.message)
            },
            onFailure = { error ->
                errorPipelineResult(error)
            }
        )
    }
}

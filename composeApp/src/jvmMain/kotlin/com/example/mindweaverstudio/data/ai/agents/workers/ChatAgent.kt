package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createErrorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createSuccessPipelineResult
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_USER


class ChatAgent(
    private val aiClient: AiClient,
) : Agent {
    override val name = CHAT_AGENT
    override val description: String = "Агент для обычного общения"
    private var history: List<ChatMessage> = emptyList()

    override suspend fun run(input: String): PipelineResult {
        val messages = history + listOf(ChatMessage(input, ROLE_USER))
        history = messages

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.7,
            maxTokens = 800,
        )
        return result.fold(
            onSuccess = { response ->
                createSuccessPipelineResult(response.message)
            },
            onFailure = { error ->
                createErrorPipelineResult(error)
            }
        )
    }
}

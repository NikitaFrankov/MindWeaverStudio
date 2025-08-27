package com.example.mindweaverstudio.data.agents

import com.example.mindweaverstudio.data.models.agents.Agent
import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.models.agents.AgentResult
import com.example.mindweaverstudio.data.models.agents.AgentResult.Companion.createErrorAgentResult
import com.example.mindweaverstudio.data.models.agents.AgentResult.Companion.createSuccessAgentResult
import com.example.mindweaverstudio.data.models.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.models.chat.ChatMessage

class ChatAgent(
    private val aiClient: AiClient,
) : Agent {
    override val name = CHAT_AGENT
    override val description: String = "Агент для обычного общения"
    private var history: List<ChatMessage> = emptyList()

    override suspend fun run(input: ChatMessage): AgentResult {
        val messages = history + listOf(input)
        history = messages

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.7,
            maxTokens = 800,
        )
        return result.fold(
            onSuccess = { response ->
                createSuccessAgentResult(message = response.message)
            },
            onFailure = { error ->
                createErrorAgentResult(error)
            }
        )
    }
}

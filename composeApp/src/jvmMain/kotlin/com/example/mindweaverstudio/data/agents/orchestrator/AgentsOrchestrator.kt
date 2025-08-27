package com.example.mindweaverstudio.data.agents.orchestrator

import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.extensions.decodeFromStringOrNull
import com.example.mindweaverstudio.data.models.agents.AgentManagementResponse
import com.example.mindweaverstudio.data.models.agents.AgentResult
import com.example.mindweaverstudio.data.models.agents.AgentResult.Companion.createErrorAgentResult
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_USER
import kotlinx.serialization.json.Json

class AgentsOrchestrator(
    private val registry: AgentsRegistry,
    private val aiClient: AiClient,
) {
    private val systemPrompt: ChatMessage = generateSystemPrompt()
    private var messagesHistory: List<ChatMessage> = listOf(systemPrompt)
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    suspend fun handleMessage(userInput: String): AgentResult {
        val userMessage = ChatMessage(ROLE_USER, userInput)
        messagesHistory += userMessage

        val aiResult = aiClient.createChatCompletion(
            messages = messagesHistory,
            temperature = 0.3,
            maxTokens = 1024
        )

        return aiResult.fold(
            onSuccess = { result ->
                val decision = json.decodeFromStringOrNull<AgentManagementResponse>(result.message)
                    ?: return@fold createErrorAgentResult(message = "Не получилось распарсить ответ от оркестратора")
                val agent = registry.get(decision.agent)
                    ?: return@fold createErrorAgentResult(message = "Неизвестный агент")

                return agent.run(input = userMessage)
            },
            onFailure = {
                return createErrorAgentResult(error = it)
            }
        )
    }

    private fun generateSystemPrompt(): ChatMessage {
        val prompt = """
            Ты — оркестратор агентов.
            Твоя задача — анализировать сообщение пользователя и решать, какой из доступных агентов должен обработать этот запрос.  
            Ни один запрос ты сам не выполняешь.  
            Ты всегда отвечаешь строго в JSON-формате.

            Вот список доступных агентов и их назначения:
            ${registry.getPresentableList()}

            JSON-формат ответа:
            {"agent": "<имя_агента_из_списка>"}

            Правила:
            - Если сообщение пользователя подходит под функционал одного из агентов → укажи этого агента.  
            - Если не подходит ни под один агент → используй "chat_agent".  
            - Никаких комментариев или текста или markdown форматирования вне JSON. 
        """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt,
        )
    }

}
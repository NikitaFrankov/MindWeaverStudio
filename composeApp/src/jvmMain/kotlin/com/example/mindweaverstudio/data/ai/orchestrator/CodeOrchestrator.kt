package com.example.mindweaverstudio.data.ai.orchestrator

import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.ai.pipelines.PipelineRegistry
import com.example.mindweaverstudio.data.utils.extensions.decodeFromStringOrNull
import com.example.mindweaverstudio.data.models.pipeline.CodeOrchestratorCommand
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createErrorPipelineResult
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_USER
import kotlinx.serialization.json.Json

class CodeOrchestrator(
    private val registry: PipelineRegistry,
    private val aiClient: AiClient,
) {
    private val systemPrompt: ChatMessage = generateSystemPrompt()
    private var messagesHistory: List<ChatMessage> = listOf(systemPrompt)
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    suspend fun handleMessage(userInput: String): PipelineResult {
        val userMessage = ChatMessage(ROLE_USER, userInput)
        messagesHistory += userMessage

        val aiResult = aiClient.createChatCompletion(
            messages = messagesHistory,
            temperature = 0.3,
            maxTokens = 1024
        )

        return aiResult.fold(
            onSuccess = { result ->
                val decision = json.decodeFromStringOrNull<CodeOrchestratorCommand>(result.message)
                    ?: return@fold createErrorPipelineResult(message = "Не получилось распарсить ответ от оркестратора")
                val pipeline = registry.get(decision.pipeline)
                    ?: return@fold createErrorPipelineResult(message = "Неизвестный агент")

                return pipeline.run(input = userMessage)
            },
            onFailure = {
                return createErrorPipelineResult(error = it)
            }
        )
    }

    private fun generateSystemPrompt(): ChatMessage {
        val prompt = """
            Ты — оркестратор пайплайнов.
            Твоя задача — анализировать сообщение пользователя и решать, какой из доступных пайплайнов должен обработать этот запрос.  
            Ни один запрос ты сам не выполняешь.  
            Ты всегда отвечаешь строго в JSON-формате.

            Вот список доступных пайплайнов и их назначения:
            ${registry.getPresentableList()}

            JSON-формат ответа:
            {"pipeline": "<имя_пайплайна_из_списка>"}

            Правила:
            - Если сообщение пользователя подходит под функционал одного из пайплайнов → укажи этот пайплайн.  
            - Если не подходит ни под один пайплайн → используй "chat_pipeline".  
            - Никаких комментариев или текста или markdown форматирования вне JSON. 
        """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt,
        )
    }

}
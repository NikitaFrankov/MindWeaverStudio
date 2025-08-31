package com.example.mindweaverstudio.data.ai.orchestrator

import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.ai.pipelines.PipelineRegistry
import com.example.mindweaverstudio.data.utils.extensions.decodeFromStringOrNull
import com.example.mindweaverstudio.data.models.pipeline.CodeOrchestratorCommand
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createErrorPipelineResult
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_USER
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
           You are a pipeline orchestrator.
            Your task is to analyze the user message and decide which of the available pipelines should handle this request.
            You never execute any request yourself.
            You always respond strictly in JSON format.
            
            Here is the list of available pipelines and their purposes:
            ${registry.getPresentableList()}
            
            JSON response format:
            {"pipeline": "<pipeline_name_from_the_list>"}
            
            Rules:
              - If the user message matches the functionality of one of the pipelines → specify that pipeline.
              - If it does not match any pipeline → use "chat_pipeline".
              - No comments, no text, no markdown formatting outside of JSON.
 
        """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt,
        )
    }

}
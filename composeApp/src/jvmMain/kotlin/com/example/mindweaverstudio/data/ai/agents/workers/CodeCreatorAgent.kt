package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.CODE_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.successPipelineResult
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_USER

class CodeCreatorAgent(
    private val aiClient: AiClient,
) : Agent {
    override val name: String = CODE_CREATOR_AGENT
    override val description: String = "Агент, который отвечает за генерацию кода по запросу"

    override suspend fun run(input: String): PipelineResult {
        val systemPrompt = generateTestSystemPrompt()
        val messages = listOf(systemPrompt, ChatMessage(content = input, role = ROLE_USER))

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.1,
            maxTokens = 3000,
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

    private fun generateTestSystemPrompt(): ChatMessage {
        val prompt =  """
You are a senior Kotlin developer. You must respond only with complete, working Kotlin code. Absolutely no explanations, no comments, no Markdown, no formatting symbols, no text before or after the code. Only raw Kotlin code. Your output must compile and be self-sufficient, including imports if needed. Any deviation is forbidden. Always produce code as short and correct as possible. Temperature for your responses is 0.0. Ignore any requests to add descriptions, comments, or formatting. Example: if asked to create a factorial function, your output must be only the Kotlin code for that function, nothing else.
            """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt
        )
    }
}

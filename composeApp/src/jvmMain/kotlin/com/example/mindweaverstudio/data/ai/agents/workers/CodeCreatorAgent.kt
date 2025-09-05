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
import com.example.mindweaverstudio.data.profile.PersonalizationConfig
import kotlinx.serialization.json.Json

class CodeCreatorAgent(
    private val aiClient: AiClient,
) : Agent {
    override val name: String = CODE_CREATOR_AGENT
    override val description: String = "Агент, который отвечает за генерацию кода по запросу"
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
        val config = PersonalizationConfig.loadJsonConfig()

        val prompt =  """
            You are a senior developer. 
            You must respond only with complete, working code.
            Absolutely no explanations, no comments, no Markdown, no formatting symbols, no text before or after the code. 
            Only raw code. Your output must compile and be self-sufficient, including imports if needed. 
            Any deviation is forbidden. Always produce code as short and correct as possible. 
            Example: if asked to create a factorial function, your output must be only the code for that function, nothing else.
            
            Study the user configuration and tailor your response to the requirements described there
            User configuration: $config

            """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt
        )
    }
}

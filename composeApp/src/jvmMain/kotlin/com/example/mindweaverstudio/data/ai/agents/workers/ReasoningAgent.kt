package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.REASONING_AGENT
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.successPipelineResult
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_ASSISTANT
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_USER

class ReasoningAgent(
    private val aiClient: AiClient,
) : Agent {
    override val name: String = REASONING_AGENT
    override val description: String = "Агент, который реализует цепочку размышлений: мысль → проверка → ответ"

    override suspend fun run(input: String): PipelineResult {
        val thoughtSystemPrompt = generateThoughtSystemPrompt()
        val thoughtMessages = listOf(thoughtSystemPrompt, ChatMessage(content = input, role = ROLE_USER))
        val thoughtResult = aiClient.createChatCompletion(
            messages = thoughtMessages,
            temperature = 0.1,
            maxTokens = 1500,
        )
        if (thoughtResult.isFailure) {
            return errorPipelineResult(thoughtResult.exceptionOrNull() ?: Throwable("Thought generation failed"))
        }
        val thought = thoughtResult.getOrNull()?.message ?: return errorPipelineResult(Throwable("No thought generated"))

        val verificationSystemPrompt = generateVerificationSystemPrompt()
        val verificationMessages = listOf(
            verificationSystemPrompt,
            ChatMessage(content = thought, role = ROLE_ASSISTANT),
            ChatMessage(content = input, role = ROLE_USER)
        )
        val verificationResult = aiClient.createChatCompletion(
            messages = verificationMessages,
            temperature = 0.1,
            maxTokens = 1500,
        )
        if (verificationResult.isFailure) {
            return errorPipelineResult(verificationResult.exceptionOrNull() ?: Throwable("Verification failed"))
        }
        val verifiedThought = verificationResult.getOrNull()?.message ?: return errorPipelineResult(Throwable("No verification result"))

        // Шаг 3: Генерация финального ответа (на основе verified thoughts и originalQuery)
        val responseSystemPrompt = generateResponseSystemPrompt()
        val responseMessages = listOf(
            responseSystemPrompt,
            ChatMessage(content = verifiedThought, role = ROLE_ASSISTANT),
            ChatMessage(content = input, role = ROLE_USER)
        )
        val responseResult = aiClient.createChatCompletion(
            messages = responseMessages,
            temperature = 0.1,
            maxTokens = 1500,
        )
        return responseResult.fold(
            onSuccess = { response ->
                successPipelineResult(message = response.message)
            },
            onFailure = { error ->
                errorPipelineResult(error)
            }
        )
    }

    private fun generateThoughtSystemPrompt(): ChatMessage {
        val prompt = """
You are an expert reasoner. For the user's query, generate step-by-step thoughts. Break down the problem logically: identify key elements, consider options, potential risks, and outline insights. Use "Let's think step by step" to structure your reasoning. Output only the thoughts, no final answer.
            """.trimIndent()
        return ChatMessage(role = ROLE_SYSTEM, content = prompt)
    }

    private fun generateVerificationSystemPrompt(): ChatMessage {
        val prompt = """
You are a critical verifier. Review the thoughts for factual errors, logical inconsistencies, missing details, or improvements. If accurate, confirm and enhance with additional insights if needed. If flawed, correct them step-by-step. Output only the verified or corrected thoughts, keeping the step-by-step structure.
            """.trimIndent()
        return ChatMessage(role = ROLE_SYSTEM, content = prompt)
    }

    private fun generateResponseSystemPrompt(): ChatMessage {
        val prompt = """
You are a concise responder. Based solely on the verified thoughts, provide a clear, accurate, and complete final answer to the user's query. Structure it logically (e.g., numbered steps if applicable), keep it brief but comprehensive. Directly address the question without unnecessary repetition.
            """.trimIndent()
        return ChatMessage(role = ROLE_SYSTEM, content = prompt)
    }
}
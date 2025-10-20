package com.example.mindweaverstudio.data.ai.pipelines.chat

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import com.example.mindweaverstudio.data.ai.models.LocalModels
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration

const val CHAT_STRATEGY = "CHAT_STRATEGY"

class ChatPipeline(config: ApiConfiguration) {

    private val chatPipelineStrategy = strategy<String, String>(CHAT_STRATEGY) {
        val nodeRequirements by node<String, String> { input: String ->
            llm.writeSession {
                model = OpenAIModels.CostOptimized.O3Mini
                updatePrompt {
                    user(input)
                }
                val response = requestLLMWithoutTools()
                response.content
            }
        }

        edge(nodeStart forwardTo nodeRequirements)
        edge(nodeRequirements forwardTo nodeFinish)
    }

    private val promptExecutor = simpleOllamaAIExecutor()
    val agent = AIAgent(
        id = CHAT_STRATEGY,
        promptExecutor = promptExecutor,
        strategy = chatPipelineStrategy,
        llmModel = LocalModels.QWEN.LLAMA_3_2_8B
    ) {
        install(Tracing)
    }

    suspend fun run(input: String): String {
        return agent.run(input)
    }
}
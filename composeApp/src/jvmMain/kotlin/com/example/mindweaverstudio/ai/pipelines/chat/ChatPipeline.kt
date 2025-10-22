package com.example.mindweaverstudio.ai.pipelines.chat

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
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

    private val promptExecutor = simpleOpenAIExecutor(config.openAiApiKey)
    val agent = AIAgent(
        id = CHAT_STRATEGY,
        promptExecutor = promptExecutor,
        strategy = chatPipelineStrategy,
        llmModel = OpenAIModels.CostOptimized.GPT4oMini
    ) {
        install(Tracing)
    }

    suspend fun run(input: String): String {
        return agent.run(input)
    }
}
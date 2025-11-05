package com.example.mindweaverstudio.ai.pipelines.chat

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration

const val CHAT_STRATEGY = "CHAT_STRATEGY"

class ChatPipeline(config: ApiConfiguration) {

    private val chatPipelineStrategy = strategy<String, String>(CHAT_STRATEGY) {
        val nodeStartChat by node<String, String> { input: String ->
            llm.writeSession {
                model = OpenAIModels.CostOptimized.GPT4oMini
                updatePrompt {
                    system(chatPipelineSystemPrompt)
                    user(input)
                }
                val response = requestLLMWithoutTools()
                response.content
            }
        }

        edge(nodeStart forwardTo nodeStartChat)
        edge(nodeStartChat forwardTo nodeFinish)
    }

    private val promptExecutor = simpleOpenAIExecutor(config.openAiApiKey)
    val agent = AIAgent(
        id = CHAT_STRATEGY,
        promptExecutor = promptExecutor,
        strategy = chatPipelineStrategy,
        llmModel = OpenAIModels.CostOptimized.GPT4oMini
    )

    suspend fun run(input: String): String {
        return agent.run(input)
    }
}
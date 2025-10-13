package com.example.mindweaverstudio.data.ai.pipelines.architecture

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration

const val ARCHITECTURE_STRATEGY = "ARCHITECTURE_STRATEGY"

class ArchitecturePipeline(config: ApiConfiguration) {

    private val architectureStrategy = strategy<String, String>(ARCHITECTURE_STRATEGY) {
        // 1. Этап: Анализ требований
        val nodeRequirements by node<String, String> { input: String ->
            llm.writeSession {
                model = OpenAIModels.CostOptimized.O3Mini
                updatePrompt {
                    system(nodeRequirementsSystemPrompt)
                    user(input)
                }
                val response = requestLLMWithoutTools()
                response.content
            }
        }

        // 2. Этап: Высокоуровневая архитектура
        val nodeHighLevel by node<String, String> { input: String ->
            llm.writeSession {
                model = OpenAIModels.CostOptimized.O3Mini
                updatePrompt {
                    system(nodeHighLevelSystemPrompt)
                    user(input)
                }
                val response = requestLLMWithoutTools()
                response.content
            }
        }

        // 3. Этап: Детальное проектирование
        val nodeDetailed by node<String, String> { input: String ->
            llm.writeSession {
                model = OpenAIModels.CostOptimized.O3Mini
                updatePrompt {
                    system(nodeDetailedSystemPrompt)
                    user(input)
                }
                val response = requestLLMWithoutTools()
                response.content
            }
        }

        // 4. Этап: Валидация и оптимизация
        val nodeValidation by node<String, String> { input: String ->
            llm.writeSession {
                model = OpenAIModels.CostOptimized.O3Mini
                updatePrompt {
                    system(nodeValidationSystemPrompt)
                    user(input)
                }
                val response = requestLLMWithoutTools()
                response.content
            }
        }

        edge(nodeStart forwardTo nodeRequirements)
        edge(nodeRequirements forwardTo nodeHighLevel)
        edge(nodeHighLevel forwardTo nodeDetailed)
        edge(nodeDetailed forwardTo nodeValidation)
        edge(nodeValidation forwardTo nodeFinish)
    }


    private val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(config.openAiApiKey),
        strategy = architectureStrategy,
        llmModel = OpenAIModels.Chat.GPT4o,
    ) {
        install(Tracing)
    }

    suspend fun run(input: String): String {
        return agent.run(input)
    }
}
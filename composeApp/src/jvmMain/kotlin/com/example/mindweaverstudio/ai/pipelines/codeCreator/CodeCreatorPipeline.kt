package com.example.mindweaverstudio.ai.pipelines.codeCreator

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.params.LLMParams
import com.example.mindweaverstudio.ai.pipelines.githubRelease.releaseNotesAgentSystemPrompt
import com.example.mindweaverstudio.ai.tools.codeCheck.CodeCheckTools
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration

const val CODE_CREATOR_STRATEGY = "CODE_CREATOR_STRATEGY"

class CodeCreatorPipeline(
    private val codeCheckTools: CodeCheckTools,
    config: ApiConfiguration,
) {
    private val llmModel = OpenAIModels.CostOptimized.O3Mini
    private val codeCreatorPipelineStrategy = strategy<String, String>(CODE_CREATOR_STRATEGY) {
        val nodeRequirements by node<String, String> { input: String ->
            llm.writeSession {
                model = llmModel
                updatePrompt {
                    system(codeCreatorSystemPrompt())
                    user(input)
                }
                val response = requestLLMWithoutTools()
                response.content
            }
        }

        val codeCheck by subgraphWithTask<String, String>(
            tools = ToolRegistry { tools(codeCheckTools) }.tools,
            llmParams = LLMParams().copy(temperature = 0.3),
            llmModel = llmModel,
        ) { releaseNotesAgentSystemPrompt }

        edge(nodeStart forwardTo nodeRequirements)
        edge(nodeRequirements forwardTo codeCheck)
        edge(nodeRequirements forwardTo nodeFinish)
    }

    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(config.openAiApiKey),
        strategy = codeCreatorPipelineStrategy,
        llmModel = OpenAIModels.CostOptimized.O3Mini
    ) {
        install(Tracing)
    }

    suspend fun run(input: String): String {
        return agent.run(input)
    }
}
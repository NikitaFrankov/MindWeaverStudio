package com.example.mindweaverstudio.data.ai.pipelines.codeFix

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.example.mindweaverstudio.data.utils.codereplacer.CodeReplacerUtils
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration

const val CODE_FIX_STRATEGY = "CODE_FIX_STRATEGY"

class CodeFixerResult(
    sourceCode: String,
    filepath: String,
    newCode: String,
)

class CodeFixPipeline(
    config: ApiConfiguration
) {

    private val codeFixPipelineStrategy = strategy<String, String>(CODE_FIX_STRATEGY) {

        val nodeFix by node<String, CodeFixerResult> { input: String ->
            llm.writeSession {
                model = OpenAIModels.CostOptimized.O3Mini
                updatePrompt {
                    system(codeFixSystemPrompt)
                    user(input)
                }

                val message = requestLLMWithoutTools()

                CodeFixerResult(
                    sourceCode = "searchResult.sourceCode",
                    filepath = "searchResult.filePath",
                    newCode = message.content
                )
            }
        }

        val nodeHighLevel by node<CodeFixerResult, String> { input: CodeFixerResult ->
            val result = CodeReplacerUtils.replaceCodeInFile(
                filePath = "",
                originalCode = "",
                newCode = "",
            )
            result
        }

        edge(nodeStart forwardTo nodeFix)
        edge(nodeFix forwardTo nodeHighLevel)
        edge(nodeHighLevel forwardTo nodeFinish)
    }

    private val promptExecutor = simpleOpenAIExecutor(config.openAiApiKey)
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = codeFixPipelineStrategy,
        llmModel = OpenAIModels.CostOptimized.O3Mini
    ) {
        install(Tracing)
    }

    suspend fun run(input: String): String {
        return agent.run(input)
    }
}
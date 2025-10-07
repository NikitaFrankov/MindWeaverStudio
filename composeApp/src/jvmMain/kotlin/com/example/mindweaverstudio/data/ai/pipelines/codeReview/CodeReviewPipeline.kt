package com.example.mindweaverstudio.data.ai.pipelines.codeReview

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.example.mindweaverstudio.data.ai.pipelines.codeFix.CodeFixerResult
import com.example.mindweaverstudio.data.ai.pipelines.codeFix.codeFixSystemPrompt
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import java.io.File

const val CODE_REVIEW_STRATEGY = "CODE_REVIEW_STRATEGY"

class CodeFixPipeline(
    config: ApiConfiguration
) {

    private val codeReviewStrategy = strategy<String, String>(CODE_REVIEW_STRATEGY) {
        var ragChunksText = ""
        val file = File("truly_streaming_output_chunks.json")
        if (file.exists()) {
            ragChunksText = file.readText()
        } else {
            println("Chunks file not found: ${file.absolutePath}")
        }
        val userMessage = ragChunksText

        val nodeReview by node<String, String> {
            llm.writeSession {
                model = OpenAIModels.CostOptimized.O3Mini
                updatePrompt {
                    system(codeReviewSystemPrompt)
                    user(userMessage)
                }

                ""
            }
        }

        codeReviewSystemPrompt

        edge(nodeStart forwardTo nodeReview)
        edge(nodeReview forwardTo nodeFinish)
    }

    private val promptExecutor = simpleOpenAIExecutor(config.openAiApiKey)
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = codeReviewStrategy,
        llmModel = OpenAIModels.CostOptimized.O3Mini
    ) {
        install(Tracing)
    }

    suspend fun run(input: String): String {
        return agent.run(input)
    }
}
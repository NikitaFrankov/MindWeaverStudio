package com.example.mindweaverstudio.data.ai.orchestrator.code

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.example.mindweaverstudio.data.ai.tools.pipelines.CodePipelineTools
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration

const val codeOrchestratorPrompt = "You are a pipeline orchestrator.Your task is to analyze the user message and decide which of the available pipelines should handle this request."

class CodeOrchestrator(
    private val tools: CodePipelineTools,
    configuration: ApiConfiguration,
) {

    private val orchestratorAgent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(configuration.openAiApiKey),
        systemPrompt = codeOrchestratorPrompt,
        llmModel = OpenAIModels.Chat.GPT4o,
        toolRegistry = ToolRegistry { tools(tools) },
    ) {
        install(Tracing)
    }

    suspend fun run(userInput: String): String {
        return orchestratorAgent.run(userInput)
    }
}
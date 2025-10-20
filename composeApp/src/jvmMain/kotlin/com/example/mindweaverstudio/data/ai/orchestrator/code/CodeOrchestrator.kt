package com.example.mindweaverstudio.data.ai.orchestrator.code

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import com.example.mindweaverstudio.data.ai.models.LocalModels
import com.example.mindweaverstudio.data.ai.tools.pipelines.CodePipelineTools
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration

const val codeOrchestratorPrompt = "You are a pipeline orchestrator.Your task is to analyze the user message and decide which of the available pipelines should handle this request."
const val CODE_ORCHESTRATOR_STRATEGY = "code_orchestrator_strategy"

class CodeOrchestrator(
    private val tools: CodePipelineTools,
    configuration: ApiConfiguration,
) {
    private val orchestratorAgent = AIAgent(
        id = CODE_ORCHESTRATOR_STRATEGY,
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = LocalModels.QWEN.LLAMA_3_2_8B,
        toolRegistry = ToolRegistry { tools(tools) },
        systemPrompt = codeOrchestratorPrompt,
    ) {
        install(Tracing)
    }

    suspend fun run(userInput: String): String {
        return orchestratorAgent.run(userInput)
    }
}
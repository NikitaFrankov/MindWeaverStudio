package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createErrorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createSuccessPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.CODE_FIX_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.Pipeline

class CodeFixPipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = CODE_FIX_PIPELINE
    override val description: String = "Pipeline, ответственностью которого является фикс багов"

    /**
     * Start code fix pipeline
     * First agent to call - [CODE_FIXER_AGENT]
     * Second agent to call - [] // TODO("Create code launcher agent")
     * Next stage - paste new code into source code
     * */
    override suspend fun run(input: ChatMessage): PipelineResult {
        val firstStep = agentsRegistry.get(CODE_FIXER_AGENT)?.run(input.content)
            ?: return createErrorPipelineResult(message = "Error during $name pipeline. First step is null")
        if (firstStep.isError) {
            return firstStep
        }

        val secondStep = agentsRegistry.get(CODE_TESTER_AGENT)?.run(firstStep.message)
            ?: return createErrorPipelineResult(message = "Error during $name pipeline. Second step is null")
        if (secondStep.isError) {
            return secondStep
        }

        return secondStep
    }
}
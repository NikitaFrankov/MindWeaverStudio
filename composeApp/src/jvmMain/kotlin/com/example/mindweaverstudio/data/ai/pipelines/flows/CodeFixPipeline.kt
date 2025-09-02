package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.CODE_FIX_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.common.Pipeline
import com.example.mindweaverstudio.data.models.pipeline.PipelineStep

/**
 * Code fix pipeline
 * First agent to call - [CODE_FIXER_AGENT]
 * Second agent to call - [CODE_TESTER_AGENT]
 * Next stage - paste new code into source code
 * */

class CodeFixPipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = CODE_FIX_PIPELINE
    override val description: String = "Pipeline, ответственностью которого является фикс багов"

    override fun steps(): List<PipelineStep> = listOf(
        PipelineStep(
            id = 1,
            name = "Code fix",
            agentName = CODE_FIXER_AGENT,
            action = { input ->
                agentsRegistry.get(CODE_FIXER_AGENT)?.run(input)
                    ?: errorPipelineResult("Error during $name running. Agent $CODE_FIXER_AGENT not found")
            }
        ),
        PipelineStep(
            id = 2,
            name = "Code test",
            agentName = CODE_TESTER_AGENT,
            action = { message ->
                agentsRegistry.get(CODE_TESTER_AGENT)?.run(message)
                    ?: errorPipelineResult("Error during $name running. Agent $CODE_TESTER_AGENT not found")
            }
        )
    )
}
package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.ARCHITECT_VALIDATOR_OPTIMIZER_AGENT
import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.DETAILED_ARCHITECT_DESIGNER_AGENT
import com.example.mindweaverstudio.data.ai.agents.HIGH_LEVEL_ARCHITECT_AGENT
import com.example.mindweaverstudio.data.ai.agents.REQUIREMENTS_ANALYST_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.ARCHITECTURE_PIPELINE
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.common.Pipeline
import com.example.mindweaverstudio.data.models.pipeline.PipelineStep

class ArchitecturePipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = ARCHITECTURE_PIPELINE
    override val description: String = "Pipeline responsible for create project architecture"

    override fun steps(): List<PipelineStep> = listOf(
        PipelineStep(
            id = 1,
            name = "Requirements analyst",
            agentName = REQUIREMENTS_ANALYST_AGENT,
            action = { input ->
                agentsRegistry.get(REQUIREMENTS_ANALYST_AGENT)?.run(input)
                    ?: errorPipelineResult("Error during $name running. Agent $REQUIREMENTS_ANALYST_AGENT not found")
            }
        ),
        PipelineStep(
            id = 2,
            name = "High level architect",
            agentName = HIGH_LEVEL_ARCHITECT_AGENT,
            action = { message ->
                agentsRegistry.get(HIGH_LEVEL_ARCHITECT_AGENT)?.run(message)
                    ?: errorPipelineResult("Error during $name running. Agent $HIGH_LEVEL_ARCHITECT_AGENT not found")
            }
        ),
        PipelineStep(
            id = 3,
            name = "Detailed architect designer",
            agentName = DETAILED_ARCHITECT_DESIGNER_AGENT,
            action = { message ->
                agentsRegistry.get(DETAILED_ARCHITECT_DESIGNER_AGENT)?.run(message)
                    ?: errorPipelineResult("Error during $name running. Agent $DETAILED_ARCHITECT_DESIGNER_AGENT not found")
            }
        ),
        PipelineStep(
            id = 4,
            name = "Architect validation and optimization",
            agentName = ARCHITECT_VALIDATOR_OPTIMIZER_AGENT,
            action = { message ->
                agentsRegistry.get(ARCHITECT_VALIDATOR_OPTIMIZER_AGENT)?.run(message)
                    ?: errorPipelineResult("Error during $name running. Agent $ARCHITECT_VALIDATOR_OPTIMIZER_AGENT not found")
            }
        ),
    )
}
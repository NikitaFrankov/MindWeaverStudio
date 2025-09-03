package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.ai.agents.REASONING_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.CHAT_PIPELINE
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.common.Pipeline
import com.example.mindweaverstudio.data.models.pipeline.PipelineStep

class ChatPipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = CHAT_PIPELINE
    override val description: String = "Pipeline, responsible for default chat with user"

    override fun steps(): List<PipelineStep> = listOf(
        PipelineStep(
            id = 1,
            name = "Reasoning",
            agentName = REASONING_AGENT,
            action = { input ->
                agentsRegistry.get(REASONING_AGENT)?.run(input)
                    ?: errorPipelineResult("Error during $name running. Agent $CHAT_AGENT not found")
            }
        ),
    )
}
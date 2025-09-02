package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.CODE_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.CODE_CREATOR_PIPELINE
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.common.Pipeline
import com.example.mindweaverstudio.data.models.pipeline.PipelineOptions
import com.example.mindweaverstudio.data.models.pipeline.PipelineStep

class CodeCreatorPipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = CODE_CREATOR_PIPELINE
    override val description: String = "Pipeline, который отвечает за генерацию кода по запросу"

    override fun steps(): List<PipelineStep> = listOf(
        PipelineStep(
            id = 1,
            name = "Code create",
            agentName = CODE_CREATOR_AGENT,
            action = { input ->
                agentsRegistry.get(CODE_CREATOR_AGENT)?.run(input)
                    ?: errorPipelineResult("Error during $name running. Agent $CODE_CREATOR_AGENT not found")
            }
        ),
    )
}
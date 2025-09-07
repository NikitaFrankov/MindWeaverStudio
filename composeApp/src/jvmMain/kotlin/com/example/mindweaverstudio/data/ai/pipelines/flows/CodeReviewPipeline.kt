package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.CODE_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_REVIEWER_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.CODE_CREATOR_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.CODE_REVIEW_PIPELINE
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.common.Pipeline
import com.example.mindweaverstudio.data.models.pipeline.PipelineOptions
import com.example.mindweaverstudio.data.models.pipeline.PipelineStep

class CodeReviewPipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = CODE_REVIEW_PIPELINE
    override val description: String = "Pipeline responsible for review of project"

    override fun steps(): List<PipelineStep> = listOf(
        PipelineStep(
            id = 1,
            name = "Code review",
            agentName = CODE_REVIEWER_AGENT,
            action = { input ->
                agentsRegistry.get(CODE_REVIEWER_AGENT)?.run(input)
                    ?: errorPipelineResult("Error during $name running. Agent $CODE_REVIEWER_AGENT not found")
            }
        ),
    )
}
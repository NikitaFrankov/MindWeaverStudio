package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.GITHUB_RELEASE_AGENT
import com.example.mindweaverstudio.data.ai.agents.RELEASE_NOTES_GENERATION_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.GITHUB_RELEASE_PIPELINE
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.common.Pipeline
import com.example.mindweaverstudio.data.models.pipeline.PipelineStep

class GithubReleasePipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = GITHUB_RELEASE_PIPELINE
    override val description: String = "Pipeline, responsible for create github release"


    override fun steps() = listOf(
        PipelineStep(
            id = 1,
            name = "Generate release notes",
            agentName = RELEASE_NOTES_GENERATION_AGENT,
            action = { input ->
                agentsRegistry.get(RELEASE_NOTES_GENERATION_AGENT)?.run(input)
                    ?: errorPipelineResult("Error during $name running. Agent $RELEASE_NOTES_GENERATION_AGENT not found")
            }
        ),
        PipelineStep(
            id = 2,
            name = "Create GitHub release",
            agentName = GITHUB_RELEASE_AGENT,
            action = { notes ->
                agentsRegistry.get(GITHUB_RELEASE_AGENT)
                    ?.run("Create github release with: $notes")
                    ?: errorPipelineResult("Error during $name running. Agent $GITHUB_RELEASE_AGENT not found")
            }
        )
    )
}
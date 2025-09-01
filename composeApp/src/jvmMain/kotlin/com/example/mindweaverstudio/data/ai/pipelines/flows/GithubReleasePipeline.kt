package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.GITHUB_RELEASE_AGENT
import com.example.mindweaverstudio.data.ai.agents.RELEASE_NOTES_GENERATION_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.GITHUB_RELEASE_PIPELINE
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.Pipeline

class GithubReleasePipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = GITHUB_RELEASE_PIPELINE
    override val description: String = "Pipeline, responsible for create github release"

    override suspend fun run(input: ChatMessage): PipelineResult {
        // First step - Create release notes
        val firstStep = agentsRegistry.get(RELEASE_NOTES_GENERATION_AGENT)?.run(input.content)
            ?: return errorPipelineResult(message = "Error during $name pipeline. First step is null")
        if (firstStep.isError) {
            return errorPipelineResult(message = "Error during $name pipeline. Error in First step - ${firstStep.message}")
        }

        // Second step - create GitHub release
        val secondStepPrompt = "Create github release with this release notes: ${firstStep.message}"
        val secondStep = agentsRegistry.get(GITHUB_RELEASE_AGENT)?.run(secondStepPrompt)
            ?: return errorPipelineResult(message = "Error during $name pipeline. Second step is null")

        return secondStep
    }
}
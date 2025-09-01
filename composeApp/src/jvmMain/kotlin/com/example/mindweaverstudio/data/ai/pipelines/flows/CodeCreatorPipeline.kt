package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.CODE_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.CODE_CREATOR_PIPELINE
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.Pipeline

class CodeCreatorPipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = CODE_CREATOR_PIPELINE
    override val description: String = "Pipeline, который отвечает за генерацию кода по запросу"

    override suspend fun run(input: ChatMessage): PipelineResult {
        val firstStep = agentsRegistry.get(CODE_CREATOR_AGENT)?.run(input.content)
            ?: return errorPipelineResult(message = "Error during $name pipeline. First step is null")

        return firstStep
    }
}
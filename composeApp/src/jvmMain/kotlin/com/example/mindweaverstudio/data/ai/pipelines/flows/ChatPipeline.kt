package com.example.mindweaverstudio.data.ai.pipelines.flows

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.CHAT_PIPELINE
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createErrorPipelineResult
import com.example.mindweaverstudio.data.ai.pipelines.Pipeline

class ChatPipeline(
    override val agentsRegistry: AgentsRegistry,
) : Pipeline {
    override val name: String = CHAT_PIPELINE
    override val description: String = "Pipeline, ответственный за чат с пользователем"

    override suspend fun run(input: ChatMessage): PipelineResult {
        val firstStep = agentsRegistry.get(CHAT_AGENT)?.run(input.content)
            ?: return createErrorPipelineResult(message = "Error during $name pipeline. First step is null")

        return firstStep
    }
}
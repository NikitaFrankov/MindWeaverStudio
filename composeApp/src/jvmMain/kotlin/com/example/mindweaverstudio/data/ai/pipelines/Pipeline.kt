package com.example.mindweaverstudio.data.ai.pipelines

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult

const val CODE_FIX_PIPELINE = "code_fix_pipeline"
const val CHAT_PIPELINE = "chat_pipeline"

interface Pipeline {
    val agentsRegistry: AgentsRegistry
    val description: String
    val name: String

    suspend fun run(input: ChatMessage): PipelineResult
}


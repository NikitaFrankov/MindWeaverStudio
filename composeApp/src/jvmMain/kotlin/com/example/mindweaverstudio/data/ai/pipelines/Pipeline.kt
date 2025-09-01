package com.example.mindweaverstudio.data.ai.pipelines

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult

const val CODE_FIX_PIPELINE = "code_fix_pipeline"
const val CHAT_PIPELINE = "chat_pipeline"
const val CODE_CREATOR_PIPELINE = "code_creator_pipeline"
const val GITHUB_RELEASE_PIPELINE = "github_release_pipeline"

interface Pipeline {
    val agentsRegistry: AgentsRegistry
    val description: String
    val name: String

    suspend fun run(input: ChatMessage): PipelineResult
}


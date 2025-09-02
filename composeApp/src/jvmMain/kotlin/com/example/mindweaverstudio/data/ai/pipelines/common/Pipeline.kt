package com.example.mindweaverstudio.data.ai.pipelines.common

import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.models.pipeline.PipelineStep

interface Pipeline {
    val agentsRegistry: AgentsRegistry
    val description: String
    val name: String

    fun steps(): List<PipelineStep>

//    suspend fun run(
//        input: ChatMessage,
//        options: PipelineOptions,
//    ): PipelineResult
}


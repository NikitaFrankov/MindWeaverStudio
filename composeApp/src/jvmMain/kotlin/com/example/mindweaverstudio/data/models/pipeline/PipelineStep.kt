package com.example.mindweaverstudio.data.models.pipeline

data class PipelineStep(
    val id: Int,
    val name: String,
    val agentName: String,
    val action: suspend (String) -> PipelineResult
)
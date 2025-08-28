package com.example.mindweaverstudio.data.models.pipeline

import kotlinx.serialization.Serializable

@Serializable
class CodeOrchestratorCommand(
    val pipeline: String,
)
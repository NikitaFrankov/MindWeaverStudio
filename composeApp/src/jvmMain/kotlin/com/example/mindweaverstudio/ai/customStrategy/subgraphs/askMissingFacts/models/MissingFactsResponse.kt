package com.example.mindweaverstudio.ai.customStrategy.subgraphs.askMissingFacts.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MissingFactsResponse")
@LLMDescription("Missing facts that need to be save in memory")
data class MissingFactsResponse(
    @property:LLMDescription("Missing facts list")
    val missingFacts: List<String>
)
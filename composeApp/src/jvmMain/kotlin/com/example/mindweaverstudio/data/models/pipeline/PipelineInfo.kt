@file:Suppress("unused")

package com.example.mindweaverstudio.data.models.pipeline

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AgentPipelineData(
    val pipeline: PipelineInfo,
    val agent: AgentInfo,
    val input: InputData,
    val output: OutputData,
    val metadata: Metadata,
) {

    companion object {
        fun createInitial(
            prompt: String,
            agentName: String
        ) =
            AgentPipelineData(
                pipeline = PipelineInfo(
                    id = "",
                    step = 1,
                    totalSteps = 2,
                ),
                agent = AgentInfo(
                    name = agentName,
                    version = "1.0"
                ),
                input = InputData(
                    prompt = prompt,
                    data = ""
                ),
                output = OutputData(
                    type = OutputType.SUMMARY,
                    data = OutputContent(
                        summary = "",
                        bullets = emptyList()
                    ),
                ),
                metadata = Metadata(
                    success = false,
                    error = "",
                    timestamp = "",
                )
            )
    }
}

@Serializable
data class PipelineInfo(
    val id: String,
    val step: Int,
    @SerialName("total_steps") val totalSteps: Int
)

@Serializable
data class AgentInfo(
    val name: String,
    val version: String
)

@Serializable
data class InputData(
    val prompt: String,
    val data: String,
)

@Serializable
data class OutputData(
    val type: OutputType,
    val data: OutputContent
)

@Serializable
enum class OutputType {
    @SerialName("summary") SUMMARY,
    @SerialName("review") REVIEW
}

@Serializable
data class OutputContent(
    val summary: String,
    val bullets: List<String>,
)

@Serializable
data class OriginalInputData(
    val text: String
)

@Serializable
data class Metadata(
    val success: Boolean,
    val error: String? = null,
    val timestamp: String
)

object AgentPipelineParser {
    private val json = Json { ignoreUnknownKeys = false; prettyPrint = true }

    fun fromJson(jsonString: String): AgentPipelineData =
        json.decodeFromString(jsonString)

    fun toJson(result: AgentPipelineData): String =
        json.encodeToString(AgentPipelineData.serializer(), result)
}


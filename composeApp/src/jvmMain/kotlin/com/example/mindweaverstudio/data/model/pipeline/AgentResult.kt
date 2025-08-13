package com.example.mindweaverstudio.data.model.pipeline

import kotlinx.serialization.Serializable

@Serializable
class AgentResult(
    val output: AgentPipelineData?,
    val agentName: String,
    val success: Boolean,
    val error: String? = null
) {

    val successMessage
        get() = when(output) {
            null -> "Error during agent \"$agentName\" working"
            else -> "Success result from agent \"$agentName\""
        }

    companion object {
        fun createSuccess(data: AgentPipelineData) =
            AgentResult(
                output = data,
                agentName = data.agent.name,
                success = true,
                error = null
            )
        fun createFailure(error: Exception, agentName: String) =
            AgentResult(
                output = null,
                success = false,
                agentName = agentName,
                error = error.message
            )
    }
}
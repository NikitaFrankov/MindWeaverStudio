package com.example.mindweaverstudio.data.models.agents

class AgentResult(
    val message: String,
    val isError: Boolean,
) {

    companion object {
        fun createSuccessAgentResult(message: String) =
            AgentResult(
                message = message,
                isError = false,
            )
        fun createErrorAgentResult(error: Throwable): AgentResult {
            val errorMessage = "Error during agent work, throwable = $error, message = ${error.message.orEmpty().ifEmpty { "Unknown error" }}"

            return AgentResult(
                message = errorMessage,
                isError = true,
            )
        }
        fun createErrorAgentResult(message: String): AgentResult {
            val errorMessage = "Error during agent work, message = ${message.ifEmpty { "Unknown error" }}"

            return AgentResult(
                message = errorMessage,
                isError = true,
            )
        }
    }
}
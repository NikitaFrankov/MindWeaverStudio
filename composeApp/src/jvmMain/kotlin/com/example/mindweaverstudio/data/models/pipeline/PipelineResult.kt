package com.example.mindweaverstudio.data.models.pipeline

class PipelineResult(
    val message: String,
    val isError: Boolean,
) {

    companion object Companion {
        fun successPipelineResult(message: String) =
            PipelineResult(
                message = message,
                isError = false,
            )
        fun errorPipelineResult(error: Throwable): PipelineResult {
            val errorMessage = "Error during agent work, throwable = $error, message = ${error.message.orEmpty().ifEmpty { "Unknown error" }}"

            return PipelineResult(
                message = errorMessage,
                isError = true,
            )
        }
        fun errorPipelineResult(message: String): PipelineResult {
            val errorMessage = "Error during agent work, message = ${message.ifEmpty { "Unknown error" }}"

            return PipelineResult(
                message = errorMessage,
                isError = true,
            )
        }
    }
}
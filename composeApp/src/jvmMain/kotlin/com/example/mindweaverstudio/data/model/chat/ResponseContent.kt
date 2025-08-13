package com.example.mindweaverstudio.data.model.chat

sealed class ResponseContent {
    data class PlainText(val text: String) : ResponseContent()
    data class Structured(val output: StructuredOutput) : ResponseContent()
    data class RequirementsSummary(val summary: com.example.mindweaverstudio.data.model.chat.RequirementsSummary) : ResponseContent()

    val resultText: String
        get() = when(this) {
            is PlainText -> text
            is RequirementsSummary -> summary.summary
            is Structured -> output.summary.text
        }
}
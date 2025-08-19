package com.example.mindweaverstudio.data.models.ai

class AiResponse(
    val message: String
) {

    companion object {
        fun createTextResponse(text: String) =
            AiResponse(message = text)
    }
}
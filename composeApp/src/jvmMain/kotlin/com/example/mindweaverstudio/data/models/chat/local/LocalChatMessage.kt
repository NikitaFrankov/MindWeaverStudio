package com.example.mindweaverstudio.data.models.chat.local

import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Запрос в Ollama /api/chat
@Serializable
data class LocalChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val options: Options? = null
)

// Дополнительные параметры (temperature, top_p, top_k и т.д.)
@Serializable
data class Options(
    @SerialName("temperature")
    var localTemperature: Double?,
    @SerialName("num_ctx")
    var numContext: Int?,
    @SerialName("top_p")
    var topP: Double?,
    @SerialName("top_k")
    var topK: Int?,
    @SerialName("repeat_penalty")
    var repeatPenalty: Double? = 1.1
)

fun Options(block: Options.() -> Unit): Options {
    return Options(
        localTemperature = null,
        numContext = null,
        topP = null,
        topK = null,
        repeatPenalty = 1.1
    ).apply(block)
}

@Serializable
data class LocalChatResponse(
    val model: String?,
    @SerialName("created_at") val createdAt: String?,
    val message: ChatMessage?,
    val done: Boolean?,
    val error: OllamaError?
)

@Serializable
data class OllamaError(
    val message: String?,
    val type: String?,
    val code: String?,
)
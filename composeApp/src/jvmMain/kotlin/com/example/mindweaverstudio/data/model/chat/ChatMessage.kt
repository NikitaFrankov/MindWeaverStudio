package com.example.mindweaverstudio.data.model.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
) {

    val presentableContent: String
        get() = content.substringAfterLast("<<<END>>>").trim()

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"
    }
}
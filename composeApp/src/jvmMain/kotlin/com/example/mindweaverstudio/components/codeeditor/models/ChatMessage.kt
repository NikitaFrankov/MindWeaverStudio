package com.example.mindweaverstudio.components.codeeditor.models

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
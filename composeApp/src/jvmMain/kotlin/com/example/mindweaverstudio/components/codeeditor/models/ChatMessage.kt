package com.example.mindweaverstudio.components.codeeditor.models

sealed class UiChatMessage {
    abstract val content: String
    abstract val timestamp: Long
    
    data class UserMessage(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UiChatMessage()
    
    data class AssistantMessage(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UiChatMessage()
    
    data class ThinkingMessage(
        override val content: String = "Thinking...",
        override val timestamp: Long = System.currentTimeMillis()
    ) : UiChatMessage()

    
    companion object {
        
        fun createUserMessage(content: String): UserMessage {
            return UserMessage(content = content)
        }
        
        fun createAssistantMessage(content: String): AssistantMessage {
            return AssistantMessage(content = content)
        }
        
        fun createThinkingMessage(): ThinkingMessage {
            return ThinkingMessage()
        }
    }
}
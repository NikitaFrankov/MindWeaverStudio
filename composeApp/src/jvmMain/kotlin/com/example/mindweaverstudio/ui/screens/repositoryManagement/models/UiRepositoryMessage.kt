package com.example.mindweaverstudio.ui.screens.repositoryManagement.models

import com.example.mindweaverstudio.data.models.chat.ChatMessage

sealed class UiRepositoryMessage {
    abstract val content: String
    abstract val timestamp: Long
    
    data class UserMessage(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UiRepositoryMessage()
    
    data class AssistantMessage(
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : UiRepositoryMessage()
    
    data class ThinkingMessage(
        override val content: String = "Thinking...",
        override val timestamp: Long = System.currentTimeMillis()
    ) : UiRepositoryMessage()
    
    fun toChatMessage(): ChatMessage {
        return when (this) {
            is UserMessage -> ChatMessage(role = "user", content = content)
            is AssistantMessage -> ChatMessage(role = "assistant", content = content)
            is ThinkingMessage -> ChatMessage(role = "assistant", content = content)
        }
    }
    
    companion object {
        fun fromChatMessage(chatMessage: ChatMessage): UiRepositoryMessage {
            return when (chatMessage.role) {
                "user" -> UserMessage(content = chatMessage.content)
                "assistant" -> AssistantMessage(content = chatMessage.content)
                else -> AssistantMessage(content = chatMessage.content)
            }
        }
        
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
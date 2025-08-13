package com.example.mindweaverstudio.ui.model

import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.model.chat.ResponseContent
import com.example.mindweaverstudio.data.model.chat.StructuredOutput

sealed class UiChatMessage {
    abstract val presentableContent: String
    abstract fun toApiMessage(): ChatMessage
    
    class UserMessage(
        override val presentableContent: String,
        val rawContent: String
    ) : UiChatMessage() {
        override fun toApiMessage(): ChatMessage {
            return ChatMessage(role = "user", content = rawContent)
        }
    }
    
    class AssistantMessage(
        override val presentableContent: String,
        val structuredOutput: StructuredOutput
    ) : UiChatMessage() {
        override fun toApiMessage(): ChatMessage {
            return ChatMessage(role = "assistant", content = presentableContent)
        }
    }
    
    class PlainTextMessage(
        override val presentableContent: String
    ) : UiChatMessage() {
        override fun toApiMessage(): ChatMessage {
            return ChatMessage(role = "assistant", content = presentableContent)
        }
    }
    
    class RequirementsSummaryMessage(
        override val presentableContent: String,
        val summary: com.example.mindweaverstudio.data.model.chat.RequirementsSummary
    ) : UiChatMessage() {
        override fun toApiMessage(): ChatMessage {
            return ChatMessage(role = "assistant", content = presentableContent)
        }
    }
    
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"
        
        fun createUserMessage(content: String): UserMessage {
            return UserMessage(
                rawContent = content,
                presentableContent = content
            )
        }
        
        fun createAssistantMessage(responseContent: ResponseContent): UiChatMessage {
            return when (responseContent) {
                is ResponseContent.PlainText -> PlainTextMessage(
                    presentableContent = responseContent.text
                )
                is ResponseContent.Structured -> AssistantMessage(
                    presentableContent = responseContent.output.summary.text,
                    structuredOutput = responseContent.output
                )
                is ResponseContent.RequirementsSummary -> RequirementsSummaryMessage(
                    presentableContent = responseContent.summary.summary,
                    summary = responseContent.summary
                )
            }
        }
        
        @Deprecated("Use createAssistantMessage with ResponseContent")
        fun createAssistantMessage(structuredOutput: StructuredOutput): AssistantMessage {
            return AssistantMessage(
                presentableContent = structuredOutput.summary.text,
                structuredOutput = structuredOutput
            )
        }
        
        fun fromApiMessage(apiMessage: ChatMessage, structuredOutput: StructuredOutput? = null): UiChatMessage {
            return when (apiMessage.role) {
                ROLE_USER -> UserMessage(
                    rawContent = apiMessage.content,
                    presentableContent = extractPresentableContent(apiMessage.content)
                )
                ROLE_ASSISTANT -> {
                    requireNotNull(structuredOutput) { "Assistant messages must have structured output" }
                    AssistantMessage(
                        presentableContent = structuredOutput.summary.text,
                        structuredOutput = structuredOutput
                    )
                }
                else -> throw IllegalArgumentException("Unsupported role: ${apiMessage.role}")
            }
        }
        
        private fun extractPresentableContent(content: String): String {
            return content.substringAfterLast("<<<END>>>").trim().ifEmpty { content }
        }
    }
}
package com.example.mindweaverstudio.components.chat

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.data.model.deepseek.ChatMessage

interface ChatStore : Store<ChatStore.Intent, ChatStore.State, ChatStore.Label> {

    data class State(
        val messages: List<ChatMessage> = emptyList(),
        val currentMessage: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedModel: String = "deepseek-chat",
        val selectedProvider: String = "DeepSeek"
    )

    sealed class Intent {
        data class UpdateMessage(val message: String) : Intent()
        data object SendMessage : Intent()
        data object ClearError : Intent()
        data object ClearChat : Intent()
        data class ChangeModel(val model: String) : Intent()
        data class ChangeProvider(val provider: String) : Intent()
    }

    sealed class Label {
        data class ShowError(val message: String) : Label()
    }
}
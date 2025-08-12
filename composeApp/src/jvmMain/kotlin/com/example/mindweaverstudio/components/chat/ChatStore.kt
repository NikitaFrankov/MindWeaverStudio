package com.example.mindweaverstudio.components.chat

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.ui.model.UiChatMessage
import com.example.mindweaverstudio.data.model.PromptMode

interface ChatStore : Store<ChatStore.Intent, ChatStore.State, ChatStore.Label> {

    data class State(
        val messages: List<UiChatMessage> = emptyList(),
        val currentMessage: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedModel: String = "deepseek-chat",
        val selectedProvider: String = "DeepSeek",
        val selectedPromptMode: String = PromptMode.DEFAULT_MODE.id
    )

    sealed class Intent {
        data class UpdateMessage(val message: String) : Intent()
        data object SendMessage : Intent()
        data object ClearError : Intent()
        data object ClearChat : Intent()
        data class ChangeModel(val model: String) : Intent()
        data class ChangeProvider(val provider: String) : Intent()
        data class ChangePromptMode(val promptModeId: String) : Intent()
    }

    sealed class Label {
        data class ShowError(val message: String) : Label()
    }
}
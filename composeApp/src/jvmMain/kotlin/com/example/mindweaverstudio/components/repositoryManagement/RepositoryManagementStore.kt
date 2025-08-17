package com.example.mindweaverstudio.components.repositoryManagement

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.ui.model.UiRepositoryMessage
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStore.Intent
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStore.Label
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStore.State

interface RepositoryManagementStore : Store<Intent, State, Label> {

    data class State(
        val messages: List<UiRepositoryMessage> = emptyList(),
        val currentMessage: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedModel: String = "gpt-3.5-turbo",
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
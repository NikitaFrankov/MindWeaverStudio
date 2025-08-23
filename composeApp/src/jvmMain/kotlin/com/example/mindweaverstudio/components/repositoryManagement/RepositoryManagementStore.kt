package com.example.mindweaverstudio.components.repositoryManagement

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.ui.screens.repositoryManagement.models.UiRepositoryMessage
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStore.Intent
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStore.Label
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStore.State

interface RepositoryManagementStore : Store<Intent, State, Label> {

    data class State(
        val messages: List<UiRepositoryMessage> = emptyList(),
        val currentMessage: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    sealed class Intent {
        data class UpdateMessage(val message: String) : Intent()
        data object SendMessage : Intent()
        data object ClearError : Intent()
        data object ClearChat : Intent()
    }

    sealed class Label {
        data class ShowError(val message: String) : Label()
    }
}
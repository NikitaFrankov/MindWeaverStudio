package com.example.mindweaverstudio.components.repoInfoInput

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputStore.*

interface RepoInfoInputStore : Store<Intent, State, Label> {

    data class State(
        val repoName: String = "",
        val repoOwner: String = ""
    )

    sealed interface Intent {
        data object OnConfirmChanges : Intent
        data object OnCancel : Intent

        class OnRepoNameChange(val newValue: String) : Intent
        class OnRepoOwnerChange(val newValue: String) : Intent
    }

    sealed interface Label {
        data object ConfirmChanges : Label
        data object CancelDialog : Label
    }
}
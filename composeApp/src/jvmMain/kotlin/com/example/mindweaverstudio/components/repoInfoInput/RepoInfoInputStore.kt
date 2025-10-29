package com.example.mindweaverstudio.components.repoInfoInput

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputStore.*
import com.example.mindweaverstudio.data.models.repository.RepositoryInfo

interface RepoInfoInputStore : Store<Intent, State, Label> {

    data class State(
        val project: Project,
        val repoInfo: RepositoryInfo = RepositoryInfo(),
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
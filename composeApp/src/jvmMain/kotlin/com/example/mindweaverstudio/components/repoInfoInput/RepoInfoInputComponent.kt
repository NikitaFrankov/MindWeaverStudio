package com.example.mindweaverstudio.components.repoInfoInput

import kotlinx.coroutines.flow.StateFlow

interface RepoInfoInputComponent {
    val state: StateFlow<RepoInfoInputStore.State>

    fun onIntent(intent: RepoInfoInputStore.Intent)

    sealed interface Callback {
        data object CloseDialog : Callback
    }
}
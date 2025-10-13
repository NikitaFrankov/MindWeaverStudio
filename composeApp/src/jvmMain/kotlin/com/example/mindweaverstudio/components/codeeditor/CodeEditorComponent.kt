package com.example.mindweaverstudio.components.codeeditor

import kotlinx.coroutines.flow.StateFlow

interface CodeEditorComponent {
    val state: StateFlow<CodeEditorStore.State>

    fun onIntent(intent: CodeEditorStore.Intent)
    fun onNavigateToUserConfiguration()

    sealed interface Callback {
        data object ShowUserConfiguration : Callback
        data object ShowRepoInfoInput: Callback
    }
}
package com.example.mindweaverstudio.components.codeeditor

import kotlinx.coroutines.flow.StateFlow

interface CodeEditorComponent {
    val state: StateFlow<CodeEditorStore.State>

    fun onIntent(intent: CodeEditorStore.Intent)
    fun onNavigateToUserConfiguration()
}
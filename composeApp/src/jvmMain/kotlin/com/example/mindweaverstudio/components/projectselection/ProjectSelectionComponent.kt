package com.example.mindweaverstudio.components.projectselection

import kotlinx.coroutines.flow.StateFlow

interface ProjectSelectionComponent {
    val state: StateFlow<ProjectSelectionStore.State>

    fun onIntent(intent: ProjectSelectionStore.Intent)
}
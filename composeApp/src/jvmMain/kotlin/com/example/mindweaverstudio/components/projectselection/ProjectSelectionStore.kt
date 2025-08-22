package com.example.mindweaverstudio.components.projectselection

import com.arkivanov.mvikotlin.core.store.Store
import kotlinx.serialization.Serializable

interface ProjectSelectionStore : Store<ProjectSelectionStore.Intent, ProjectSelectionStore.State, ProjectSelectionStore.Label> {

    data class State(
        val projects: List<Project> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    sealed class Intent {
        object LoadRecentProjects : Intent()
        object SelectNewProject : Intent()
        class OpenProject(val project: Project) : Intent()
        class RemoveProject(val path: String) : Intent()
        class AddRecentProject(val path: String) : Intent()
    }

    sealed class Label {
        class ShowError(val message: String) : Label()
        class ProjectSelected(val project: Project) : Label()
        object ShowFilePicker : Label()
    }
}

@Serializable
data class Project(
    val path: String,
    val name: String,
    val lastOpened: Long
)
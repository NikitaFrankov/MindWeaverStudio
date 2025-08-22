package com.example.mindweaverstudio.components.projectselection

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

class ProjectSelectionStoreFactory(
    private val storeFactory: StoreFactory,
) {

    fun create(): ProjectSelectionStore =
        object : ProjectSelectionStore, Store<ProjectSelectionStore.Intent, ProjectSelectionStore.State, ProjectSelectionStore.Label> by storeFactory.create(
            name = "ProjectSelectionStore",
            initialState = ProjectSelectionStore.State(),
            bootstrapper = Bootstrapper(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Action {
        data object LoadRecentProjects : Action()
    }

    private sealed class Msg {
        data class RecentProjectsLoaded(val projects: List<Project>) : Msg()
        data class LoadingChanged(val isLoading: Boolean) : Msg()
        data class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
    }

    private inner class Bootstrapper: CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.LoadRecentProjects)
        }
    }

    private inner class ExecutorImpl : CoroutineExecutor<ProjectSelectionStore.Intent, Action, ProjectSelectionStore.State, Msg, ProjectSelectionStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        private val preferences = Preferences.userNodeForPackage(ProjectSelectionStoreFactory::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        override fun executeIntent(intent: ProjectSelectionStore.Intent) {
            when (intent) {
                is ProjectSelectionStore.Intent.LoadRecentProjects -> loadRecentProjects()
                
                is ProjectSelectionStore.Intent.SelectNewProject -> {
                    showFilePicker()
                }
                
                is ProjectSelectionStore.Intent.OpenProject -> {
                    publish(ProjectSelectionStore.Label.ProjectSelected(intent.project))
                }
                
                is ProjectSelectionStore.Intent.RemoveProject -> {
                    removeProject(intent.path)
                }
                
                is ProjectSelectionStore.Intent.AddRecentProject -> {
                    addRecentProject(intent.path)
                }
            }
        }

        override fun executeAction(action: Action) {
            when (action) {
                is Action.LoadRecentProjects -> loadRecentProjects()
            }
        }

        private fun loadRecentProjects() {
            dispatch(Msg.LoadingChanged(true))
            scope.launch {
                try {
                    val projectsJson = preferences.get("recent_projects", "[]")
                    val storedProjects = json.decodeFromString<List<StoredProject>>(projectsJson)
                    
                    val projects = storedProjects
                        .filter { File(it.path).exists() } // Only include existing directories
                        .map { stored ->
                            Project(
                                path = stored.path,
                                name = File(stored.path).name.takeIf { it.isNotEmpty() } ?: stored.path,
                                lastOpened = stored.lastOpened
                            )
                        }
                        .sortedByDescending { it.lastOpened }
                        .take(10) // Limit to 10 recent projects
                    
                    // Clean up preferences if we filtered out non-existing projects
                    if (projects.size != storedProjects.size) {
                        saveRecentProjects(projects)
                    }
                    
                    dispatch(Msg.RecentProjectsLoaded(projects))
                } catch (e: Exception) {
                    dispatch(Msg.ErrorOccurred("Failed to load recent projects: ${e.message}"))
                } finally {
                    dispatch(Msg.LoadingChanged(false))
                }
            }
        }

        private fun addRecentProject(path: String) {
            scope.launch {
                try {
                    val currentProjects = state().projects.toMutableList()
                    
                    // Remove if already exists
                    currentProjects.removeAll { it.path == path }
                    
                    // Add at the beginning
                    val newProject = Project(
                        path = path,
                        name = File(path).name.takeIf { it.isNotEmpty() } ?: path,
                        lastOpened = System.currentTimeMillis()
                    )
                    currentProjects.add(0, newProject)
                    
                    // Keep only last 10
                    val limitedProjects = currentProjects.take(10)
                    
                    saveRecentProjects(limitedProjects)
                    dispatch(Msg.RecentProjectsLoaded(limitedProjects))
                    
                    // Navigate to the project
                    publish(ProjectSelectionStore.Label.ProjectSelected(newProject))
                } catch (e: Exception) {
                    dispatch(Msg.ErrorOccurred("Failed to add recent project: ${e.message}"))
                }
            }
        }

        private fun removeProject(path: String) {
            scope.launch {
                try {
                    val updatedProjects = state().projects.filter { it.path != path }
                    saveRecentProjects(updatedProjects)
                    dispatch(Msg.RecentProjectsLoaded(updatedProjects))
                } catch (e: Exception) {
                    dispatch(Msg.ErrorOccurred("Failed to remove project: ${e.message}"))
                }
            }
        }

        private fun saveRecentProjects(projects: List<Project>) {
            val storedProjects = projects.map { 
                StoredProject(path = it.path, lastOpened = it.lastOpened) 
            }
            val json = json.encodeToString(storedProjects)
            preferences.put("recent_projects", json)
        }

        private fun showFilePicker() {
            SwingUtilities.invokeLater {
                val fileChooser = JFileChooser().apply {
                    dialogTitle = "Select Project Directory"
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isAcceptAllFileFilterUsed = false
                    
                    // Set current directory to user home or last opened project directory
                    val lastProject = state().projects.firstOrNull()
                    if (lastProject != null) {
                        val lastProjectDir = File(lastProject.path).parentFile
                        if (lastProjectDir?.exists() == true) {
                            currentDirectory = lastProjectDir
                        }
                    } else {
                        currentDirectory = File(System.getProperty("user.home"))
                    }
                }
                
                val result = fileChooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    val selectedDirectory = fileChooser.selectedFile
                    if (selectedDirectory != null && selectedDirectory.isDirectory) {
                        // Add to recent projects and navigate
                        scope.launch {
                            try {
                                addRecentProject(selectedDirectory.absolutePath)
                            } catch (e: Exception) {
                                dispatch(Msg.ErrorOccurred("Failed to select project: ${e.message}"))
                            }
                        }
                    }
                }
            }
        }
    }

    private object ReducerImpl : Reducer<ProjectSelectionStore.State, Msg> {
        override fun ProjectSelectionStore.State.reduce(msg: Msg): ProjectSelectionStore.State =
            when (msg) {
                is Msg.RecentProjectsLoaded -> copy(projects = msg.projects, error = null)
                is Msg.LoadingChanged -> copy(isLoading = msg.isLoading)
                is Msg.ErrorOccurred -> copy(error = msg.error)
                is Msg.ErrorCleared -> copy(error = null)
            }
    }
}

@Serializable
private data class StoredProject(
    val path: String,
    val lastOpened: Long
)
package com.example.mindweaverstudio.components.repoInfoInput

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputStore.*
import com.example.mindweaverstudio.data.models.repository.RepositoryInfo
import com.example.mindweaverstudio.data.settings.Settings
import com.example.mindweaverstudio.data.settings.SettingsKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.json.Json

class RepoInfoInputStoreFactory(
    private val storeFactory: StoreFactory,
    private val settings: Settings
) {

    private val json = Json {  }

    fun create(project: Project): RepoInfoInputStore =
        object : RepoInfoInputStore, Store<Intent, State, Label> by storeFactory.create(
            name = "ProjectSelectionStore",
            initialState = State(project = project),
            bootstrapper = SimpleBootstrapper(Action.Init),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Msg {
        class CurrentRepositoryInfo(val value: RepositoryInfo) : Msg()
        class RepoOwnerUpdates(val newValue: String) : Msg()
        class RepoNameUpdates(val newValue: String) : Msg()
    }

    private sealed interface Action {
        data object Init : Action
    }

    private inner class ExecutorImpl : CoroutineExecutor<Intent, Action, State, Msg, Label>(
        mainContext = Dispatchers.Swing,
    ) {

        override fun executeIntent(intent: Intent) = when(intent) {
            is Intent.OnRepoOwnerChange ->  dispatch(Msg.RepoOwnerUpdates(intent.newValue))
            is Intent.OnRepoNameChange -> dispatch(Msg.RepoNameUpdates(intent.newValue))
            is Intent.OnCancel -> publish(Label.CancelDialog)
            is Intent.OnConfirmChanges -> confirmChanges()
        }

        override fun executeAction(action: Action) = when(action) {
            Action.Init -> fetchRepoInformation()
        }

        private fun fetchRepoInformation() {
            scope.launch {
                val repoPath = state().project.path
                val repoInfoString = settings.getString(key = SettingsKey.ProjectRepoInformation(repoPath))
                val repoInfo = when(repoInfoString.isEmpty()) {
                    true -> RepositoryInfo()
                    else -> json.decodeFromString(
                        deserializer = RepositoryInfo.serializer(),
                        string = repoInfoString,
                    )
                }

                dispatch(Msg.CurrentRepositoryInfo(repoInfo))
            }
        }

        private fun confirmChanges() {
            scope.launch {
                val repoPath = state().project.path
                val repoInfo = json.encodeToString(
                    serializer = RepositoryInfo.serializer(),
                    value = state().repoInfo,
                )
                settings.putString(key = SettingsKey.ProjectRepoInformation(repoPath), repoInfo)

                publish(Label.ConfirmChanges)
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State =
            when (msg) {
                is Msg.RepoNameUpdates -> copy(repoInfo = repoInfo.copy(name = msg.newValue))
                is Msg.RepoOwnerUpdates -> copy(repoInfo = repoInfo.copy(owner = msg.newValue))
                is Msg.CurrentRepositoryInfo -> copy(repoInfo = msg.value)
            }
    }
}
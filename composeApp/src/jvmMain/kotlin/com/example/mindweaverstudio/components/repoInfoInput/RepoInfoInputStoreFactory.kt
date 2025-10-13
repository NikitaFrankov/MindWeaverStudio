package com.example.mindweaverstudio.components.repoInfoInput

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputStore.*
import com.example.mindweaverstudio.data.settings.Settings
import com.example.mindweaverstudio.data.settings.SettingsKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

class RepoInfoInputStoreFactory(
    private val storeFactory: StoreFactory,
    private val settings: Settings
) {

    fun create(): RepoInfoInputStore =
        object : RepoInfoInputStore, Store<Intent, State, Label> by storeFactory.create(
            name = "ProjectSelectionStore",
            initialState = State(),
            bootstrapper = SimpleBootstrapper(Action.Init),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Msg {
        data class RepoNameUpdates(val newValue: String) : Msg()
        data class RepoOwnerUpdates(val newValue: String) : Msg()
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
                val (repoName, repoOwner) = settings.getString(key = SettingsKey.GITHUB_REPO_NAME) to
                        settings.getString(key = SettingsKey.GITHUB_REPO_OWNER)

                dispatch(Msg.RepoOwnerUpdates(repoOwner))
                dispatch(Msg.RepoNameUpdates(repoName))
            }
        }

        private fun confirmChanges() {
            scope.launch {
                val (newRepoOwner, newRepoName) = state().repoOwner to state().repoName
                val (currentRepoName, currentRepoOwner) = settings.getString(key = SettingsKey.GITHUB_REPO_NAME) to
                        settings.getString(key = SettingsKey.GITHUB_REPO_OWNER)

                if (newRepoOwner != currentRepoOwner) {
                    settings.putString(SettingsKey.GITHUB_REPO_OWNER, state().repoOwner)
                }
                if (newRepoName != currentRepoName) {
                    settings.putString(SettingsKey.GITHUB_REPO_NAME, state().repoName)
                }
                publish(Label.ConfirmChanges)
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State =
            when (msg) {
                is Msg.RepoNameUpdates -> copy(repoName = msg.newValue)
                is Msg.RepoOwnerUpdates -> copy(repoOwner = msg.newValue)
            }
    }
}
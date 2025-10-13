package com.example.mindweaverstudio.components.repoInfoInput

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DefaultRepoInfoInputComponent(
    private val storeFactory: RepoInfoInputStoreFactory,
    private val callbackHandler: (RepoInfoInputComponent.Callback) -> Unit,
    componentContext: ComponentContext,
) : RepoInfoInputComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        storeFactory.create()
    }
    private val componentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        componentScope.launch {
            store.labels.collect { label ->
                when (label) {
                    is RepoInfoInputStore.Label.ConfirmChanges, is RepoInfoInputStore.Label.CancelDialog ->
                        callbackHandler.invoke(RepoInfoInputComponent.Callback.CloseDialog)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<RepoInfoInputStore.State> = store.stateFlow

    override fun onIntent(intent: RepoInfoInputStore.Intent) {
        store.accept(intent)
    }
}
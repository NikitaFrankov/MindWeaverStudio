package com.example.mindweaverstudio.components.projectselection

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionComponent.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DefaultProjectSelectionComponent(
    private val projectSelectionStoreFactory: ProjectSelectionStoreFactory,
    private val callbackHandler: (Callback) -> Unit,
    componentContext: ComponentContext,
) : ProjectSelectionComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        projectSelectionStoreFactory.create()
    }
    
    private val componentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        store.labels
            .onEach { label ->
                when (label) {
                    is ProjectSelectionStore.Label.ProjectSelected -> {
                        callbackHandler.invoke(Callback.ProjectSelected(project = label.project))
                    }
                    is ProjectSelectionStore.Label.ShowError -> {
                        // Error handling could be implemented here if needed
                    }
                    is ProjectSelectionStore.Label.ShowFilePicker -> {
                        // File picker is handled within the store
                    }
                }
            }
            .launchIn(componentScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ProjectSelectionStore.State> = store.stateFlow

    override fun onIntent(intent: ProjectSelectionStore.Intent) {
        store.accept(intent)
    }
}
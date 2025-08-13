package com.example.mindweaverstudio.components.pipeline

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

class DefaultPipelineComponent(
    private val pipelineStoreFactory: PipelineStoreFactory,
    componentContext: ComponentContext,
) : PipelineComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        pipelineStoreFactory.create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<PipelineStore.State> = store.stateFlow

    override fun onIntent(intent: PipelineStore.Intent) {
        store.accept(intent)
    }
}
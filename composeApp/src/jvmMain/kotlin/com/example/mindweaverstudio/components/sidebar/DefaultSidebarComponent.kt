package com.example.mindweaverstudio.components.sidebar

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

class DefaultSidebarComponent(
    private val sidebarStoreFactory: SidebarStoreFactory,
    componentContext: ComponentContext,
) : SidebarComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        sidebarStoreFactory.create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<SidebarStore.State> = store.stateFlow

    override fun onIntent(intent: SidebarStore.Intent) {
        store.accept(intent)
    }
}
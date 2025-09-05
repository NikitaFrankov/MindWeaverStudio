package com.example.mindweaverstudio.components.userconfiguration

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

class DefaultUserConfigurationComponent(
    private val userConfigurationStoreFactory: UserConfigurationStoreFactory,
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit
) : UserConfigurationComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        userConfigurationStoreFactory.create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<UserConfigurationStore.State> = store.stateFlow

    override fun onIntent(intent: UserConfigurationStore.Intent) {
        store.accept(intent)
    }

    override fun onBackPressed() {
        onNavigateBack()
    }
}
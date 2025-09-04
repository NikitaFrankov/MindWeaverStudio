package com.example.mindweaverstudio.components.authentication

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DefaultAuthenticationComponent(
    private val authenticationStoreFactory: AuthenticationStoreFactory,
    componentContext: ComponentContext,
    private val onAuthenticationSuccessful: () -> Unit,
) : AuthenticationComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        authenticationStoreFactory.create()
    }
    
    private val componentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        store.labels
            .onEach { label ->
                when (label) {
                    is AuthenticationStore.Label.AuthenticationSuccessful -> {
                        onAuthenticationSuccessful()
                    }
                    is AuthenticationStore.Label.ShowError -> {
                        // Error handling could be implemented here if needed
                        // For now, errors are handled through state
                    }
                }
            }
            .launchIn(componentScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<AuthenticationStore.State> = store.stateFlow

    override fun onIntent(intent: AuthenticationStore.Intent) {
        store.accept(intent)
    }
}
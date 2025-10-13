package com.example.mindweaverstudio.components.authentication

import kotlinx.coroutines.flow.StateFlow

interface AuthenticationComponent {
    val state: StateFlow<AuthenticationStore.State>

    fun onIntent(intent: AuthenticationStore.Intent)

    sealed interface Callback {
        data object SuccessAuthentification : Callback
    }
}
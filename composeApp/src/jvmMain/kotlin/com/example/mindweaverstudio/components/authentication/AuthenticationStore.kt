package com.example.mindweaverstudio.components.authentication

import com.arkivanov.mvikotlin.core.store.Store

interface AuthenticationStore : Store<AuthenticationStore.Intent, AuthenticationStore.State, AuthenticationStore.Label> {

    data class State(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isEmailValid: Boolean = true,
        val isPasswordValid: Boolean = true,
        val isAuthenticated: Boolean = false
    )

    sealed class Intent {
        data class UpdateEmail(val email: String) : Intent()
        data class UpdatePassword(val password: String) : Intent()
        object SignIn : Intent()
        object ClearError : Intent()
    }

    sealed class Label {
        object AuthenticationSuccessful : Label()
        data class ShowError(val message: String) : Label()
    }
}
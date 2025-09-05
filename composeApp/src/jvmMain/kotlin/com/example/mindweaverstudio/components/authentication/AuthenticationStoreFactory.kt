package com.example.mindweaverstudio.components.authentication

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.data.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.util.prefs.Preferences

class AuthenticationStoreFactory(
    private val storeFactory: StoreFactory,
    private val authManager: AuthManager,
) {

    fun create(): AuthenticationStore =
        object : AuthenticationStore, Store<AuthenticationStore.Intent, AuthenticationStore.State, AuthenticationStore.Label> by storeFactory.create(
            name = "AuthenticationStore",
            initialState = AuthenticationStore.State(),
            bootstrapper = Bootstrapper(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Action {
        data object CheckAuthenticationStatus : Action()
    }

    private sealed class Msg {
        data class EmailUpdated(val email: String, val isValid: Boolean) : Msg()
        data class PasswordUpdated(val password: String, val isValid: Boolean) : Msg()
        data class LoadingChanged(val isLoading: Boolean) : Msg()
        data class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
        data object AuthenticationSucceeded : Msg()
    }

    private inner class Bootstrapper: CoroutineBootstrapper<Action>() {
        override fun invoke() {
            dispatch(Action.CheckAuthenticationStatus)
        }
    }

    private inner class ExecutorImpl : CoroutineExecutor<AuthenticationStore.Intent, Action, AuthenticationStore.State, Msg, AuthenticationStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        private val preferences = Preferences.userNodeForPackage(AuthenticationStoreFactory::class.java)

        override fun executeIntent(intent: AuthenticationStore.Intent) {
            when (intent) {
                is AuthenticationStore.Intent.UpdateEmail -> {
                    val isValid = isValidEmail(intent.email)
                    dispatch(Msg.EmailUpdated(intent.email, isValid))
                }
                
                is AuthenticationStore.Intent.UpdatePassword -> {
                    val isValid = isValidPassword(intent.password)
                    dispatch(Msg.PasswordUpdated(intent.password, isValid))
                }
                
                is AuthenticationStore.Intent.SignIn -> {
                    signIn()
                }
                
                is AuthenticationStore.Intent.ClearError -> {
                    dispatch(Msg.ErrorCleared)
                }
            }
        }

        override fun executeAction(action: Action) {
            when (action) {
                is Action.CheckAuthenticationStatus -> checkAuthenticationStatus()
            }
        }

        private fun checkAuthenticationStatus() {
            scope.launch {
                try {
                    val isAuthenticated = preferences.getBoolean("is_authenticated", false)
                    if (isAuthenticated) {
                        dispatch(Msg.AuthenticationSucceeded)
                        publish(AuthenticationStore.Label.AuthenticationSuccessful)
                    }
                } catch (e: Exception) {
                    // Ignore errors during status check
                }
            }
        }

        private fun signIn() {
            val state = state()
            
            if (!state.isEmailValid || !state.isPasswordValid) {
                dispatch(Msg.ErrorOccurred("Please enter valid email and password"))
                return
            }
            
            if (state.email.isBlank()) {
                dispatch(Msg.ErrorOccurred("Email is required"))
                return
            }
            
            if (state.password.isBlank()) {
                dispatch(Msg.ErrorOccurred("Password is required"))
                return
            }

            dispatch(Msg.LoadingChanged(true))
            
            scope.launch {
                try {
                    val token = authManager.generateToken(state.email, state.password)

                    if (token != null) {
                        dispatch(Msg.AuthenticationSucceeded)
                        publish(AuthenticationStore.Label.AuthenticationSuccessful)
                    } else {
                        dispatch(Msg.ErrorOccurred("Invalid credentials"))
                    }
                } catch (e: Exception) {
                    dispatch(Msg.ErrorOccurred("Authentication failed: ${e.message}"))
                } finally {
                    dispatch(Msg.LoadingChanged(false))
                }
            }
        }

        private fun isValidEmail(email: String): Boolean {
            return email.isNotBlank()
        }

        private fun isValidPassword(password: String): Boolean {
            return password.length >= 2
        }
    }

    private object ReducerImpl : Reducer<AuthenticationStore.State, Msg> {
        override fun AuthenticationStore.State.reduce(msg: Msg): AuthenticationStore.State =
            when (msg) {
                is Msg.EmailUpdated -> copy(email = msg.email, isEmailValid = msg.isValid, error = null)
                is Msg.PasswordUpdated -> copy(password = msg.password, isPasswordValid = msg.isValid, error = null)
                is Msg.LoadingChanged -> copy(isLoading = msg.isLoading)
                is Msg.ErrorOccurred -> copy(error = msg.error)
                is Msg.ErrorCleared -> copy(error = null)
                is Msg.AuthenticationSucceeded -> copy(
                    isAuthenticated = true,
                    isLoading = false,
                    error = null
                )
            }
    }
}
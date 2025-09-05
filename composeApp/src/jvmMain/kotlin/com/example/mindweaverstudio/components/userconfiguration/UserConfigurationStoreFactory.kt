package com.example.mindweaverstudio.components.userconfiguration

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.data.profile.PersonalizationConfig
import com.example.mindweaverstudio.data.models.profile.UserPersonalization
import com.example.mindweaverstudio.components.userconfiguration.UserConfigurationStore.Msg
import com.example.mindweaverstudio.components.userconfiguration.UserConfigurationStore.Msg.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

class UserConfigurationStoreFactory(
    private val storeFactory: StoreFactory,
) {

    fun create(): UserConfigurationStore =
        object : UserConfigurationStore, Store<UserConfigurationStore.Intent, UserConfigurationStore.State, UserConfigurationStore.Label> by storeFactory.create(
            name = "UserConfigurationStore",
            initialState = UserConfigurationStore.State(),
            bootstrapper = SimpleBootstrapper(UserConfigurationStore.Action.Init),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<UserConfigurationStore.Intent, UserConfigurationStore.Action, UserConfigurationStore.State, Msg, UserConfigurationStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        override fun executeAction(action: UserConfigurationStore.Action) = when(action) {
            UserConfigurationStore.Action.Init -> {
                loadUserConfiguration()
            }
        }

        override fun executeIntent(intent: UserConfigurationStore.Intent) = when(intent) {
            is UserConfigurationStore.Intent.UpdateName -> {
                dispatch(NameUpdated(intent.name))
                checkForUnsavedChanges()
            }
            is UserConfigurationStore.Intent.UpdateRole -> {
                dispatch(RoleUpdated(intent.role))
                checkForUnsavedChanges()
            }
            is UserConfigurationStore.Intent.UpdatePreferredLanguage -> {
                dispatch(PreferredLanguageUpdated(intent.language))
                checkForUnsavedChanges()
            }
            is UserConfigurationStore.Intent.UpdateResponseFormat -> {
                dispatch(ResponseFormatUpdated(intent.format))
                checkForUnsavedChanges()
            }
            is UserConfigurationStore.Intent.UpdateExperienceLevel -> {
                dispatch(ExperienceLevelUpdated(intent.level))
                checkForUnsavedChanges()
            }
            is UserConfigurationStore.Intent.UpdateTimeZone -> {
                dispatch(TimeZoneUpdated(intent.timeZone))
                checkForUnsavedChanges()
            }
            UserConfigurationStore.Intent.SaveConfiguration -> {
                saveUserConfiguration()
            }
            UserConfigurationStore.Intent.ResetConfiguration -> {
                resetToDefaults()
            }
            UserConfigurationStore.Intent.ClearError -> {
                dispatch(ErrorCleared)
            }
        }

        private fun loadUserConfiguration() {
            dispatch(LoadingStarted)
            scope.launch {
                try {
                    val userPersonalization = PersonalizationConfig.load()
                    dispatch(UserPersonalizationLoaded(userPersonalization))
                } catch (e: Exception) {
                    dispatch(ErrorOccurred("Failed to load user configuration: ${e.message}"))
                } finally {
                    dispatch(LoadingFinished)
                }
            }
        }

        private fun saveUserConfiguration() {
            dispatch(ConfigurationSaveStarted)
            scope.launch {
                try {
                    PersonalizationConfig.save(state().userPersonalization)
                    dispatch(ConfigurationSaved)
                    dispatch(UnsavedChangesUpdated(false))
                    publish(UserConfigurationStore.Label.ConfigurationSaved)
                } catch (e: Exception) {
                    dispatch(ErrorOccurred("Failed to save user configuration: ${e.message}"))
                }
            }
        }

        private fun resetToDefaults() {
            val defaultPersonalization = UserPersonalization()
            dispatch(UserPersonalizationLoaded(defaultPersonalization))
            checkForUnsavedChanges()
        }

        private fun checkForUnsavedChanges() {
            scope.launch {
                try {
                    val savedConfig = PersonalizationConfig.load()
                    val currentConfig = state().userPersonalization
                    val hasChanges = savedConfig != currentConfig
                    dispatch(UnsavedChangesUpdated(hasChanges))
                } catch (e: Exception) {
                    // If we can't load saved config, assume we have changes
                    dispatch(UnsavedChangesUpdated(true))
                }
            }
        }
    }

    private object ReducerImpl : Reducer<UserConfigurationStore.State, Msg> {
        override fun UserConfigurationStore.State.reduce(msg: Msg): UserConfigurationStore.State =
            when (msg) {
                is UserPersonalizationLoaded -> copy(
                    userPersonalization = msg.userPersonalization,
                    hasUnsavedChanges = false
                )
                is NameUpdated -> copy(
                    userPersonalization = userPersonalization.copy(name = msg.name)
                )
                is RoleUpdated -> copy(
                    userPersonalization = userPersonalization.copy(role = msg.role)
                )
                is PreferredLanguageUpdated -> copy(
                    userPersonalization = userPersonalization.copy(preferredLanguage = msg.language)
                )
                is ResponseFormatUpdated -> copy(
                    userPersonalization = userPersonalization.copy(responseFormat = msg.format)
                )
                is ExperienceLevelUpdated -> copy(
                    userPersonalization = userPersonalization.copy(experienceLevel = msg.level)
                )
                is TimeZoneUpdated -> copy(
                    userPersonalization = userPersonalization.copy(timeZone = msg.timeZone)
                )
                is ConfigurationSaveStarted -> copy(
                    isSaving = true,
                    error = null
                )
                is ConfigurationSaved -> copy(
                    isSaving = false,
                    error = null
                )
                is ErrorOccurred -> copy(
                    error = msg.error,
                    isLoading = false,
                    isSaving = false
                )
                is ErrorCleared -> copy(
                    error = null
                )
                is LoadingStarted -> copy(
                    isLoading = true,
                    error = null
                )
                is LoadingFinished -> copy(
                    isLoading = false
                )
                is UnsavedChangesUpdated -> copy(
                    hasUnsavedChanges = msg.hasChanges
                )
            }
    }
}
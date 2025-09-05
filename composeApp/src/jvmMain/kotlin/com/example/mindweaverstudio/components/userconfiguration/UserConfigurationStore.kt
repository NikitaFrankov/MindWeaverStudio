package com.example.mindweaverstudio.components.userconfiguration

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.data.models.profile.UserPersonalization
import com.example.mindweaverstudio.data.models.profile.WorkRole
import com.example.mindweaverstudio.data.models.profile.ResponseFormat
import com.example.mindweaverstudio.data.models.profile.ExperienceLevel

interface UserConfigurationStore : Store<UserConfigurationStore.Intent, UserConfigurationStore.State, UserConfigurationStore.Label> {

    data class State(
        val userPersonalization: UserPersonalization = UserPersonalization(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSaving: Boolean = false,
        val hasUnsavedChanges: Boolean = false
    )

    sealed class Intent {
        data class UpdateName(val name: String) : Intent()
        data class UpdateRole(val role: WorkRole) : Intent()
        data class UpdatePreferredLanguage(val language: String) : Intent()
        data class UpdateResponseFormat(val format: ResponseFormat) : Intent()
        data class UpdateExperienceLevel(val level: ExperienceLevel) : Intent()
        data class UpdateTimeZone(val timeZone: String) : Intent()
        data object SaveConfiguration : Intent()
        data object ResetConfiguration : Intent()
        data object ClearError : Intent()
    }

    sealed class Label {
        data object ConfigurationSaved : Label()
        data class NavigationRequested(val destination: String) : Label()
    }

    sealed interface Action {
        data object Init : Action
    }

    sealed class Msg {
        data class UserPersonalizationLoaded(val userPersonalization: UserPersonalization) : Msg()
        data class NameUpdated(val name: String) : Msg()
        data class RoleUpdated(val role: WorkRole) : Msg()
        data class PreferredLanguageUpdated(val language: String) : Msg()
        data class ResponseFormatUpdated(val format: ResponseFormat) : Msg()
        data class ExperienceLevelUpdated(val level: ExperienceLevel) : Msg()
        data class TimeZoneUpdated(val timeZone: String) : Msg()
        data object ConfigurationSaveStarted : Msg()
        data object ConfigurationSaved : Msg()
        data class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
        data object LoadingStarted : Msg()
        data object LoadingFinished : Msg()
        data class UnsavedChangesUpdated(val hasChanges: Boolean) : Msg()
    }
}
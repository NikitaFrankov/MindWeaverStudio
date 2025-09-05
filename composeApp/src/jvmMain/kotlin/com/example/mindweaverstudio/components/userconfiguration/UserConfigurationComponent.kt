package com.example.mindweaverstudio.components.userconfiguration

import kotlinx.coroutines.flow.StateFlow

interface UserConfigurationComponent {
    val state: StateFlow<UserConfigurationStore.State>

    fun onIntent(intent: UserConfigurationStore.Intent)
    fun onBackPressed()
}
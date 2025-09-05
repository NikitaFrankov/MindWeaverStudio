package com.example.mindweaverstudio.components.sidebar

import kotlinx.coroutines.flow.StateFlow

interface SidebarComponent {
    val state: StateFlow<SidebarStore.State>

    fun onIntent(intent: SidebarStore.Intent)
}
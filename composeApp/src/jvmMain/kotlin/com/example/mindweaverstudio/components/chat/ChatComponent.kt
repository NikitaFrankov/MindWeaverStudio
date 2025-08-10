package com.example.mindweaverstudio.components.chat

import kotlinx.coroutines.flow.StateFlow

interface ChatComponent {
    val state: StateFlow<ChatStore.State>

    fun onIntent(intent: ChatStore.Intent)
}
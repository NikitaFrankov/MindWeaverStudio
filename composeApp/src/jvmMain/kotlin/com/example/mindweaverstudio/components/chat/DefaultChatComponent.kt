package com.example.mindweaverstudio.components.chat

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

class DefaultChatComponent(
    private val chatStoreFactory: ChatStoreFactory,
    componentContext: ComponentContext,
) : ChatComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        chatStoreFactory.create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ChatStore.State> = store.stateFlow

    override fun onIntent(intent: ChatStore.Intent) {
        store.accept(intent)
    }
}
package com.example.mindweaverstudio.components.chat

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

class DefaultChatComponent(
    private val neuralNetworkRepository: NeuralNetworkRepository,
    private val storeFactory: StoreFactory,
    componentContext: ComponentContext,
) : ChatComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ChatStoreFactory(
            neuralNetworkRepository = neuralNetworkRepository,
            storeFactory = storeFactory,
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ChatStore.State> = store.stateFlow

    override fun onIntent(intent: ChatStore.Intent) {
        store.accept(intent)
    }
}
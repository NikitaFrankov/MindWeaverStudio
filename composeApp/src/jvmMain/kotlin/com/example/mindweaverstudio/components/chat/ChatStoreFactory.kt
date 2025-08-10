package com.example.mindweaverstudio.components.chat

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.data.model.deepseek.ChatMessage
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val neuralNetworkRepository: NeuralNetworkRepository
) {

    fun create(): ChatStore =
        object : ChatStore, Store<ChatStore.Intent, ChatStore.State, ChatStore.Label> by storeFactory.create(
            name = "ChatStore",
            initialState = ChatStore.State(),
            bootstrapper = BootstrapperImpl(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Action

    private sealed class Msg {
        data class UpdateMessage(val message: String) : Msg()
        data object MessageSent : Msg()
        data class MessagesUpdated(val messages: List<ChatMessage>) : Msg()
        data class LoadingChanged(val isLoading: Boolean) : Msg()
        data class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
        data object ChatCleared : Msg()
        data class ModelChanged(val model: String) : Msg()
        data class ProviderChanged(val provider: String) : Msg()
    }

    private class BootstrapperImpl : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            // Initial setup if needed
        }
    }

    private inner class ExecutorImpl : CoroutineExecutor<ChatStore.Intent, Action, ChatStore.State, Msg, ChatStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        override fun executeIntent(intent: ChatStore.Intent) {
            when (intent) {
                is ChatStore.Intent.UpdateMessage -> {
                    dispatch(Msg.UpdateMessage(intent.message))
                }

                ChatStore.Intent.SendMessage -> {
                    val currentState = state()
                    if (currentState.currentMessage.isNotBlank() && !currentState.isLoading) {
                        sendMessage(currentState.currentMessage, currentState.messages, currentState.selectedModel)
                    }
                }

                ChatStore.Intent.ClearError -> {
                    dispatch(Msg.ErrorCleared)
                }

                ChatStore.Intent.ClearChat -> {
                    dispatch(Msg.ChatCleared)
                }

                is ChatStore.Intent.ChangeModel -> {
                    dispatch(Msg.ModelChanged(intent.model))
                }

                is ChatStore.Intent.ChangeProvider -> {
                    dispatch(Msg.ProviderChanged(intent.provider))
                }
            }
        }

        private fun sendMessage(message: String, currentMessages: List<ChatMessage>, model: String) {
            dispatch(Msg.LoadingChanged(true))
            dispatch(Msg.MessageSent)

            val userMessage = ChatMessage(ChatMessage.ROLE_USER, message)
            val updatedMessages = currentMessages + userMessage
            dispatch(Msg.MessagesUpdated(updatedMessages))

            scope.launch {
                val result = neuralNetworkRepository.sendMessage(updatedMessages, model)
                result.fold(
                    onSuccess = { response ->
                        val assistantMessage = ChatMessage(ChatMessage.ROLE_ASSISTANT, response)
                        dispatch(Msg.MessagesUpdated(updatedMessages + assistantMessage))
                        dispatch(Msg.LoadingChanged(false))
                    },
                    onFailure = { error ->
                        dispatch(Msg.ErrorOccurred(error.message ?: "Unknown error occurred"))
                        dispatch(Msg.LoadingChanged(false))
                        publish(ChatStore.Label.ShowError(error.message ?: "Unknown error occurred"))
                    }
                )
            }
        }
    }

    private object ReducerImpl : Reducer<ChatStore.State, Msg> {
        override fun ChatStore.State.reduce(msg: Msg): ChatStore.State =
            when (msg) {
                is Msg.UpdateMessage -> copy(currentMessage = msg.message)
                Msg.MessageSent -> copy(currentMessage = "")
                is Msg.MessagesUpdated -> copy(messages = msg.messages)
                is Msg.LoadingChanged -> copy(isLoading = msg.isLoading)
                is Msg.ErrorOccurred -> copy(error = msg.error)
                Msg.ErrorCleared -> copy(error = null)
                Msg.ChatCleared -> copy(messages = emptyList(), currentMessage = "", error = null)
                is Msg.ModelChanged -> copy(selectedModel = msg.model)
                is Msg.ProviderChanged -> copy(selectedProvider = msg.provider)
            }
    }
}
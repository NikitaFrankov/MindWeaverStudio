package com.example.mindweaverstudio.components.chat

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.model.chat.ResponseContent
import com.example.mindweaverstudio.data.model.PromptMode
import com.example.mindweaverstudio.services.SystemPromptService
import com.example.mindweaverstudio.services.RepositoryProvider
import com.example.mindweaverstudio.ui.model.UiChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val repositoryProvider: RepositoryProvider,
    private val systemPromptService: SystemPromptService
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
        data class MessagesUpdated(val messages: List<UiChatMessage>) : Msg()
        data class LoadingChanged(val isLoading: Boolean) : Msg()
        data class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
        data object ChatCleared : Msg()
        data class ModelChanged(val model: String) : Msg()
        data class ProviderChanged(val provider: String) : Msg()
        data class PromptModeChanged(val promptModeId: String) : Msg()
        data object RequirementsGatheringStarted : Msg()
        data object RequirementsGatheringEnded : Msg()
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
                is ChatStore.Intent.UpdateMessage -> dispatch(Msg.UpdateMessage(intent.message))

                is ChatStore.Intent.SendMessage -> {
                    val currentState = state()
                    if (currentState.currentMessage.isNotBlank() && !currentState.isLoading) {
                        sendMessage(currentState.currentMessage, currentState.messages, currentState.selectedModel)
                    }
                }

                is ChatStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)

                is ChatStore.Intent.ClearChat -> dispatch(Msg.ChatCleared)

                is ChatStore.Intent.ChangeModel -> dispatch(Msg.ModelChanged(intent.model))

                is ChatStore.Intent.ChangeProvider -> dispatch(Msg.ProviderChanged(intent.provider))

                is ChatStore.Intent.ChangePromptMode -> {
                    // Prevent mode changes during requirements gathering
                    if (!state().isInRequirementsGathering) {
                        dispatch(Msg.PromptModeChanged(intent.promptModeId))
                        
                        // Start requirements gathering if switching to that mode
                        if (intent.promptModeId == PromptMode.REQUIREMENTS_GATHERING_MODE.id) {
                            dispatch(Msg.RequirementsGatheringStarted)
                        }
                    }
                }
                
                is ChatStore.Intent.StartRequirementsGathering -> dispatch(Msg.RequirementsGatheringStarted)
                
                is ChatStore.Intent.EndRequirementsGathering -> dispatch(Msg.RequirementsGatheringEnded)
            }
        }

        private fun sendMessage(message: String, currentMessages: List<UiChatMessage>, model: String) {
            startMessageSending(message, currentMessages)
            
            scope.launch {
                try {
                    val repository = repositoryProvider.getRepository(state().selectedProvider)
                    val apiMessages = prepareApiMessages(message, currentMessages)
                    val result = repository.sendMessage(apiMessages, model)
                    
                    handleApiResponse(result)
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }
        
        private fun startMessageSending(message: String, currentMessages: List<UiChatMessage>) {
            dispatch(Msg.LoadingChanged(true))
            dispatch(Msg.MessageSent)
            
            val userUiMessage = UiChatMessage.createUserMessage(message)
            dispatch(Msg.MessagesUpdated(currentMessages + userUiMessage))
        }
        
        private fun prepareApiMessages(message: String, currentMessages: List<UiChatMessage>): List<ChatMessage> {
            val systemPrompt = systemPromptService.getSystemPrompt(state().selectedPromptMode)
            val systemMessage = ChatMessage(ChatMessage.ROLE_SYSTEM, systemPrompt)
            val userMessage = ChatMessage(ChatMessage.ROLE_USER, message)
            
            return listOf(systemMessage) + currentMessages.map { it.toApiMessage() } + userMessage
        }
        
        private fun handleApiResponse(result: Result<ResponseContent>) {
            result.fold(
                onSuccess = { responseContent ->
                    val assistantUiMessage = UiChatMessage.createAssistantMessage(responseContent)
                    dispatch(Msg.MessagesUpdated(state().messages + assistantUiMessage))
                    dispatch(Msg.LoadingChanged(false))
                    
                    // End requirements gathering if we received a requirements summary
                    if (responseContent is ResponseContent.RequirementsSummary && state().isInRequirementsGathering) {
                        dispatch(Msg.RequirementsGatheringEnded)
                        dispatch(Msg.PromptModeChanged(PromptMode.DEFAULT_MODE.id))
                    }
                },
                onFailure = { error ->
                    handleError(error)
                }
            )
        }
        
        private fun handleError(error: Throwable) {
            val errorMessage = error.message ?: "Unknown error occurred"
            dispatch(Msg.ErrorOccurred(errorMessage))
            dispatch(Msg.LoadingChanged(false))
            publish(ChatStore.Label.ShowError(errorMessage))
        }
    }

    private object ReducerImpl : Reducer<ChatStore.State, Msg> {
        override fun ChatStore.State.reduce(msg: Msg): ChatStore.State =
            when (msg) {
                is Msg.UpdateMessage -> copy(currentMessage = msg.message)
                is Msg.MessageSent -> copy(currentMessage = "")
                is Msg.MessagesUpdated -> copy(messages = msg.messages)
                is Msg.LoadingChanged -> copy(isLoading = msg.isLoading)
                is Msg.ErrorOccurred -> copy(error = msg.error)
                is Msg.ErrorCleared -> copy(error = null)
                is Msg.ChatCleared -> copy(messages = emptyList(), currentMessage = "", error = null)
                is Msg.ModelChanged -> copy(selectedModel = msg.model)
                is Msg.ProviderChanged -> copy(
                    selectedProvider = msg.provider,
                    selectedModel = when (msg.provider) {
                        "DeepSeek" -> "deepseek-chat"
                        "ChatGPT" -> "gpt-3.5-turbo"
                        "Gemini" -> "gemini-1.5-flash"
                        else -> "deepseek-chat"
                    }
                )
                is Msg.PromptModeChanged -> copy(selectedPromptMode = msg.promptModeId)
                is Msg.RequirementsGatheringStarted -> copy(isInRequirementsGathering = true)
                is Msg.RequirementsGatheringEnded -> copy(isInRequirementsGathering = false)
            }
    }
}
package com.example.mindweaverstudio.components.repositoryManagement

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStoreFactory.Msg.*
import com.example.mindweaverstudio.services.RepositoryProvider
import com.example.mindweaverstudio.ui.model.UiRepositoryMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.network.MCPClient
import com.example.mindweaverstudio.data.network.ToolCall
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.Json

class RepositoryManagementStoreFactory(
    private val storeFactory: StoreFactory,
    private val repositoryProvider: RepositoryProvider,
    private val mcpClient: MCPClient,
) {

    fun create(): RepositoryManagementStore =
        object : RepositoryManagementStore,
            Store<RepositoryManagementStore.Intent, RepositoryManagementStore.State, RepositoryManagementStore.Label> by storeFactory.create(
                name = "RepositoryManagementStore",
                initialState = RepositoryManagementStore.State(),
                bootstrapper = BootstrapperImpl(),
                executorFactory = ::ExecutorImpl,
                reducer = ReducerImpl
            ) {}

    private sealed class Action

    private sealed class Msg {
        data class UpdateMessage(val message: String) : Msg()
        data object MessageSent : Msg()
        data class MessagesUpdated(val messages: List<UiRepositoryMessage>) : Msg()
        data class LoadingChanged(val isLoading: Boolean) : Msg()
        data class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
        data object ChatCleared : Msg()
        data class ModelChanged(val model: String) : Msg()
        data class ProviderChanged(val provider: String) : Msg()
    }

    private inner class BootstrapperImpl : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                mcpClient.init()
            }
        }
    }

    private inner class ExecutorImpl :
        CoroutineExecutor<RepositoryManagementStore.Intent, Action, RepositoryManagementStore.State, Msg, RepositoryManagementStore.Label>(
            mainContext = Dispatchers.Swing
        ) {
        override fun executeIntent(intent: RepositoryManagementStore.Intent) {
            when (intent) {
                is RepositoryManagementStore.Intent.UpdateMessage -> dispatch(UpdateMessage(intent.message))

                is RepositoryManagementStore.Intent.SendMessage -> {
                    val currentState = state()
                    if (currentState.currentMessage.isNotBlank() && !currentState.isLoading) {
                        sendMessage(currentState.currentMessage, currentState.messages, currentState.selectedModel)
                    }
                }

                is RepositoryManagementStore.Intent.ClearError -> dispatch(ErrorCleared)

                is RepositoryManagementStore.Intent.ClearChat -> dispatch(ChatCleared)

                is RepositoryManagementStore.Intent.ChangeModel -> dispatch(ModelChanged(intent.model))

                is RepositoryManagementStore.Intent.ChangeProvider -> dispatch(ProviderChanged(intent.provider))
            }
        }

        private fun sendMessage(
            message: String,
            currentMessages: List<UiRepositoryMessage>,
            selectedModel: String,
            isToolCall: Boolean = false
        ) {
            val userMessage = UiRepositoryMessage.createUserMessage(message)
            val thinkingMessage = UiRepositoryMessage.createThinkingMessage()

            // Add user message and thinking placeholder
            if (!isToolCall) {
                val updatedMessages = currentMessages + userMessage + thinkingMessage
                dispatch(MessagesUpdated(updatedMessages))
                dispatch(MessageSent)
                dispatch(LoadingChanged(true))
            }

            scope.launch {
                try {
                    val repository = repositoryProvider.getRepository(state().selectedProvider)
                    val apiMessages = prepareApiMessages(message, currentMessages, isToolCall)

                    val result = repository.sendMessage(
                        messages = apiMessages,
                        model = selectedModel,
                        temperature = 0.7,
                        maxTokens = 3800
                    )

                    result.fold(
                        onSuccess = { responseContent ->
                            if (!isToolCall && responseContent.resultText.contains("{")) {
                                handleToolCall(responseContent.resultText)
                                return@fold
                            }


                            // Remove thinking message and add actual response
                            val finalMessages = if (isToolCall) {
                                (currentMessages + UiRepositoryMessage.createAssistantMessage(responseContent.resultText))
                            } else {
                                (currentMessages + userMessage +
                                        UiRepositoryMessage.createAssistantMessage(responseContent.resultText))
                            }

                            dispatch(MessagesUpdated(finalMessages))
                            dispatch(LoadingChanged(false))
                        },
                        onFailure = { error ->
                            // Remove thinking message and show error
                            val errorMessages = currentMessages + userMessage
                            dispatch(MessagesUpdated(errorMessages))
                            dispatch(ErrorOccurred(error.message ?: "Unknown error occurred"))
                            dispatch(LoadingChanged(false))
                        }
                    )
                } catch (e: Exception) {
                    // Remove thinking message and show error
                    val errorMessages = currentMessages + userMessage
                    dispatch(MessagesUpdated(errorMessages))
                    dispatch(ErrorOccurred(e.message ?: "Unknown error occurred"))
                    dispatch(LoadingChanged(false))
                }
            }
        }

        private suspend fun handleToolCall(resultText: String) {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val toolCall = json.decodeFromString(ToolCall.serializer(), resultText)
            val toolResult = mcpClient.callTool(toolCall)
                .orEmpty()
                .joinToString { "\n\n" + it.text.orEmpty() }

            val string = """
                Проведи аналитику следующих данных:
                $toolResult
            """.trimIndent()

            val messages = buildList {
                addAll(state().messages)
                removeIf { it is UiRepositoryMessage.ThinkingMessage }
            }

            sendMessage(
                message = string,
                currentMessages = messages,
                selectedModel = state().selectedModel,
                isToolCall = true,
            )
        }
    }

    private object ReducerImpl : Reducer<RepositoryManagementStore.State, Msg> {
        override fun RepositoryManagementStore.State.reduce(msg: Msg): RepositoryManagementStore.State =
            when (msg) {
                is UpdateMessage -> copy(currentMessage = msg.message)
                is MessageSent -> copy(currentMessage = "")
                is MessagesUpdated -> copy(messages = msg.messages)
                is LoadingChanged -> copy(isLoading = msg.isLoading)
                is ErrorOccurred -> copy(error = msg.error, isLoading = false)
                is ErrorCleared -> copy(error = null)
                is ChatCleared -> copy(
                    messages = emptyList(),
                    currentMessage = "",
                    error = null,
                    isLoading = false
                )

                is ModelChanged -> copy(selectedModel = msg.model)
                is ProviderChanged -> copy(selectedProvider = msg.provider)
            }
    }

    private suspend fun prepareApiMessages(
        message: String,
        currentMessages: List<UiRepositoryMessage>,
        isToolCall: Boolean,
    ): List<ChatMessage> {
        val tools = mcpClient.getTools()
        val systemPrompt = generateSystemPrompt(tools)
        val systemMessage = ChatMessage(ChatMessage.ROLE_SYSTEM, systemPrompt)
        val userMessage = ChatMessage(ChatMessage.ROLE_USER, message)

        return if (isToolCall) {
            (currentMessages.map { it.toChatMessage() } + userMessage)
        } else {
            listOf(systemMessage) + (currentMessages.map { it.toChatMessage() } + userMessage)
        }
    }

    fun extractBetween(text: String, open: String, close: String): String? {
        val s = text.indexOf(open).takeIf { it >= 0 } ?: return null
        val e = text.indexOf(close, s + open.length).takeIf { it > s } ?: return null
        return text.substring(s + open.length, e).trim()
    }

    fun generateSystemPrompt(tools: List<Tool>): String {
        val toolsString = tools.mapIndexed { index, tool ->
            """
              "name": "${tool.name}",
              "description": "${tool.description}",
              "inputSchema": ${tool.inputSchema},
            """.trimIndent()
        }


        return """
            Ты работаешь в режиме агент+инструменты для работы с репозиторием.
            Информация о репозитории: owner = NikitaFrankov, repo = MindWeaverStudio
        
            У тебя есть список доступных инструментов (tools), которые можно вызвать через MCP.
            Вот список:
            $toolsString
            
           
            Формат вызова инструмента строго определён и не подлежит изменению
           
            {
              "action": "call_tool",
              "tool": "<название_инструмента (name)>",
              "params": { <ключи и значения параметров (inputSchema)> }
            }
        
            
            Вот пример ответа:
      
            {
              "action": "call_tool",
              "tool": "get_commits",
              "params": { "owner": "NikitaFrankov", "repo": "MindWeaverStudio" }
            }
        
            
            Требования:
            - Если для ответа нужно вызвать инструмент — возвращай ТОЛЬКО этот JSON, без текста, без комментариев, без форматирования сверху или снизу.
            - Не добавляй никакого текста или объяснений вокруг JSON.
            - Всегда соблюдай правильный JSON-формат.
            - Если для ответа не нужен вызов инструмента — верни обычный текстовый ответ (без JSON).
            - Никогда не смешивай текст и JSON в одном ответе.
            - Никогда не придумывай инструменты, которых нет в списке.
            - Если MCP вызов был сделан и данные вернулись, ты можешь анализировать их и отвечать в свободном текстовом формате.
            
            Помни: твоя задача — либо вызвать инструмент (строго JSON), либо выдать текстовый ответ. Ничего между этим быть не может.
        """.trimIndent()
    }
}

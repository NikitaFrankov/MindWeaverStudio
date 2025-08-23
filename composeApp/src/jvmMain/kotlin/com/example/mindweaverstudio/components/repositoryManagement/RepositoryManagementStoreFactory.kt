package com.example.mindweaverstudio.components.repositoryManagement

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStoreFactory.Msg.*
import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.extensions.getToolReportFormat
import com.example.mindweaverstudio.ui.screens.repositoryManagement.models.UiRepositoryMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.data.models.mcp.base.ToolCall
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.Json
import kotlin.collections.plus

class RepositoryManagementStoreFactory(
    private val githubMcpClient: GithubMCPClient,
    private val storeFactory: StoreFactory,
    private val aiClient: AiClient,
) {

    fun create(): RepositoryManagementStore = object : RepositoryManagementStore, Store<RepositoryManagementStore.Intent, RepositoryManagementStore.State, RepositoryManagementStore.Label> by storeFactory.create(
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
    }

    private inner class BootstrapperImpl : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                githubMcpClient.init()
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
                        sendMessage(currentState.currentMessage, currentState.messages)
                    }
                }

                is RepositoryManagementStore.Intent.ClearError -> dispatch(ErrorCleared)

                is RepositoryManagementStore.Intent.ClearChat -> dispatch(ChatCleared)
            }
        }

        private fun sendAssistantMessage(
            message: String,
            currentMessages: List<UiRepositoryMessage>,
        ) {
            scope.launch {
                try {
                    val apiMessages = prepareApiMessages(
                        currentMessages = currentMessages,
                        isAssistantMessage = true,
                        message = message,
                    )

                    val result = aiClient.createChatCompletion(messages = apiMessages,)

                    result.fold(
                        onSuccess = { responseContent ->
                            val message = responseContent.message

                            val finalMessages = (currentMessages + UiRepositoryMessage.createAssistantMessage(message))

                            dispatch(MessagesUpdated(finalMessages))
                            dispatch(LoadingChanged(false))
                        },
                        onFailure = { error ->
                            // Remove thinking message and show error
                            val errorMessages = currentMessages
                            dispatch(MessagesUpdated(errorMessages))
                            dispatch(ErrorOccurred(error.message ?: "Unknown error occurred"))
                            dispatch(LoadingChanged(false))
                        }
                    )
                } catch (e: Exception) {
                    // Remove thinking message and show error
                    val errorMessages = currentMessages
                    dispatch(MessagesUpdated(errorMessages))
                    dispatch(ErrorOccurred(e.message ?: "Unknown error occurred"))
                    dispatch(LoadingChanged(false))
                }
            }
        }

        private fun sendMessage(
            message: String,
            currentMessages: List<UiRepositoryMessage>,
        ) {
            val userMessage = UiRepositoryMessage.createUserMessage(message)
            val thinkingMessage = UiRepositoryMessage.createThinkingMessage()

            // Add user message and thinking placeholder
            val updatedMessages = currentMessages + userMessage + thinkingMessage
            dispatch(MessagesUpdated(updatedMessages))
            dispatch(MessageSent)
            dispatch(LoadingChanged(true))

            scope.launch {
                try {
                    val apiMessages = prepareApiMessages(message, currentMessages)

                    val result = aiClient.createChatCompletion(
                        messages = apiMessages,
                    )

                    result.fold(
                        onSuccess = { responseContent ->
                            val message = responseContent.message

                            if (message.contains("call_tool")) {
                                handleToolCall(message)
                                return@fold
                            }

                            val finalMessages = (currentMessages + userMessage + UiRepositoryMessage.createAssistantMessage(message))

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
            val toolResult = githubMcpClient.callTool(toolCall)
                .orEmpty()
                .joinToString { "\n\n" + it.text.orEmpty() }

            val string = """
                Данные из tool "${toolCall.tool}":
                $toolResult
                
                Проведи анализ данных, не добавляй форматирование mardown
            """.trimIndent()

            val messages = buildList {
                addAll(state().messages)
                removeIf { it is UiRepositoryMessage.ThinkingMessage }
            }

            sendAssistantMessage(
                message = string,
                currentMessages = messages,
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
            }
    }

    private suspend fun prepareApiMessages(
        message: String,
        currentMessages: List<UiRepositoryMessage>,
        isAssistantMessage: Boolean = false,
    ): List<ChatMessage> {
        val tools = githubMcpClient.getTools()
        val systemPrompt = generateSystemPrompt(tools)
        val systemMessage = ChatMessage(ChatMessage.ROLE_SYSTEM, systemPrompt)
        val role = when(isAssistantMessage) {
            true -> ChatMessage.ROLE_ASSISTANT
            false -> ChatMessage.ROLE_USER
        }
        val newMessage = ChatMessage(role = role, content = message)

        return listOf(systemMessage) + (currentMessages.map { it.toChatMessage() } + newMessage)
    }

    fun generateSystemPrompt(tools: List<Tool>): String {
        val toolsString = tools.mapIndexed { index, tool ->
            """
             {
              "name": "${tool.name}",
              "description": "${tool.description}",
              "inputSchema": ${tool.inputSchema},
              "report_format": ${tool.getToolReportFormat()}
              },
            """.trimIndent()
        }

        return """
            Ты работаешь в режиме агента с инструментами для работы с репозиторием.
            Информация о репозитории:
                owner = NikitaFrankov
                repo = MindWeaverStudio
            
            У тебя есть список доступных инструментов (tools), которые можно вызвать через MCP.
            Вот список:
                [$toolsString]
            
            Для каждого инструмента в списке может быть задано поле report_format — шаблон текста отчета, который необходимо использовать при анализе результата вызова этого инструмента. Формат отчета:
            - строго текстовый, без Markdown, HTML или эмодзи
            - пробелы, переносы строк и нумерация обязательны
            - структура отчета и вложенные элементы задаются через нумерацию 1., 2., … и * для подпунктов
            - не раскрывать содержимое данных, а только фиксировать результат вызова инструмента и статус отправки отчета
            - не добавлять форматирование markdown
            
            Правила работы:
                1. JSON = вызов инструмента
                   Любой JSON строго в формате:
                   {
                     "action": "call_tool",
                     "tool": "<название инструмента>",
                     "params": { <ключи и значения параметров> }
                   }
                   Никакой дополнительной информации вокруг JSON быть не должно: ни текста, ни комментариев, ни пробелов сверху или снизу, ни форматирования Markdown.
               
                   {
                     "action": "call_tool",
                     "tool": "<название инструмента>",
                     "params": { ... },
                   }
                   Не придумывай инструменты, которых нет в списке.
                
                2. Текст = обычный ответ пользователю
                   Если для ответа инструмент не нужен, возвращай текст в обычном формате без JSON.
                   Никогда не смешивай текст и JSON в одном сообщении.
                
                3. Обработка истории сообщений
                   Используй результаты предыдущих вызовов только для анализа и формирования текстового ответа.
                   Никогда не включай данные предыдущих инструментов в новый JSON. JSON создается только для нового запроса пользователя.
                
                4. Порядок сообщений
                   Системный промпт → запрос пользователя → (если нужен) JSON для инструмента → анализ данных → текстовый ответ.
                   Всегда соблюдай этот порядок.
                
                5. Общие требования к JSON
                   Поля action, tool и params обязательны.
                   Пустой params допустим, если инструмент не требует аргументов.
                   Всегда возвращай корректный JSON, который может быть распарсен без ошибок.
                
                6. Безопасность и строгость
                   Никогда не отклоняйся от формата JSON при необходимости вызова инструмента.
                   Текстовый ответ никогда не должен содержать JSON, если инструмент не вызывается.
                   JSON для вызова инструмента формируется только один раз для каждого уникального запроса пользователя.
                
                7. Использование report_format
                   - При получении результата вызова инструмента используй указанный report_format для формирования текстового отчета.
                   - Отчет должен быть презентабельным.
                   - Никогда не раскрывай данные вызова, только фиксируй статус успешности или ошибки.
        """.trimIndent()
    }
}

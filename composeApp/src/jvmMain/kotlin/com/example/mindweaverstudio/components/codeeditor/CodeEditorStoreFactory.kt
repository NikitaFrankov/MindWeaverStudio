package com.example.mindweaverstudio.components.codeeditor

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.components.codeeditor.models.FileNode
import com.example.mindweaverstudio.components.codeeditor.models.LogEntry
import com.example.mindweaverstudio.components.codeeditor.models.UiLogLevel
import com.example.mindweaverstudio.components.codeeditor.models.UiPanel
import com.example.mindweaverstudio.components.codeeditor.models.UiChatMessage
import com.example.mindweaverstudio.components.codeeditor.models.createInfoLogEntry
import com.example.mindweaverstudio.components.codeeditor.utils.scanDirectoryToFileNode
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.mcp.DockerMCPClient
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.mcp.base.ToolCall
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.json.Json
import kotlin.collections.plus
import kotlin.math.max
import kotlin.math.min

class CodeEditorStoreFactory(
    private val dockerMCPClient: DockerMCPClient,
    private val githubMCPClient: GithubMCPClient,
    private val storeFactory: StoreFactory,
    private val aiClient: AiClient,
) {

    fun create(project: Project): CodeEditorStore =
        object : CodeEditorStore, Store<CodeEditorStore.Intent, CodeEditorStore.State, CodeEditorStore.Label> by storeFactory.create(
            name = "CodeEditorStore",
            initialState = CodeEditorStore.State(project = project),
            bootstrapper = SimpleBootstrapper(Action.Init),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed interface Action {
        data object Init : Action
    }

    private sealed class Msg {
        class FileSelected(val file: FileNode) : Msg()
        class EditorContentUpdated(val content: String) : Msg()
        class ChatInputUpdated(val input: String) : Msg()
        class ChatMessageAdded(val message: UiChatMessage) : Msg()
        class MessagesUpdated(val messages: List<UiChatMessage>) : Msg()
        class LoadingChanged(val isLoading: Boolean) : Msg()
        class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
        class PanelWidthUpdated(val uiPanel: UiPanel, val width: Float) : Msg()
        class BottomPanelHeightUpdated(val height: Float) : Msg()
        class LogEntryAdded(val entry: LogEntry) : Msg()
        class OnNodesReceived(val node: FileNode) : Msg()
        class ProjectTreeUpdated(val tree: List<FileNode>) : Msg()
    }

    private inner class ExecutorImpl : CoroutineExecutor<CodeEditorStore.Intent, Action, CodeEditorStore.State, Msg, CodeEditorStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        override fun executeAction(action: Action) = when(action) {
            Action.Init -> {
                initMcpServer()
                fetchRootNode(state().project.path)
            }
        }

        private fun initMcpServer() {
            scope.launch {
                dockerMCPClient.init()
                githubMCPClient.init()
            }
        }

        private fun fetchRootNode(filePath: String) {
            scope.launch {
                val node = scanDirectoryToFileNode(rootPathStr = filePath)
                dispatch(Msg.OnNodesReceived(node))
            }
        }

        private fun toggleFolderExpansion(nodes: List<FileNode>, targetPath: String): List<FileNode> {
            return nodes.map { node ->
                if (node.path == targetPath && node.isDirectory) {
                    node.copy(expanded = !node.expanded)
                } else if (node.isDirectory && node.children.isNotEmpty()) {
                    node.copy(children = toggleFolderExpansion(node.children, targetPath))
                } else {
                    node
                }
            }
        }

        override fun executeIntent(intent: CodeEditorStore.Intent) {
            when (intent) {
                is CodeEditorStore.Intent.SelectFile -> {
                    if (!intent.file.isDirectory) {
                        dispatch(Msg.FileSelected(intent.file))
                        dispatch(Msg.EditorContentUpdated(intent.file.content.orEmpty()))
                        dispatch(
                            Msg.LogEntryAdded(
                                LogEntry(
                                    "Opened file: ${intent.file.name}",
                                    UiLogLevel.INFO
                                )
                            )
                        )
                    }
                }
                
                is CodeEditorStore.Intent.ToggleFolderExpanded -> {
                    val currentTree = state().projectTree
                    val updatedTree = toggleFolderExpansion(currentTree, intent.folderPath)
                    dispatch(Msg.ProjectTreeUpdated(updatedTree))
                }
                
                is CodeEditorStore.Intent.UpdateEditorContent -> {
                    dispatch(Msg.EditorContentUpdated(intent.content))
                }
                
                is CodeEditorStore.Intent.UpdateChatInput -> {
                    dispatch(Msg.ChatInputUpdated(intent.input))
                }
                
                is CodeEditorStore.Intent.SendChatMessage -> {
                    val currentState = state()
                    if (currentState.chatInput.isNotBlank() && !currentState.isLoading) {
                        sendMessage(currentState.chatInput, currentState.chatMessages)
                    }
                }
                
                is CodeEditorStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)

                is CodeEditorStore.Intent.UpdatePanelWidth -> {
                    val clampedWidth = min(0.8f, max(0.1f, intent.width))
                    dispatch(Msg.PanelWidthUpdated(intent.uiPanel, clampedWidth))
                }
                
                is CodeEditorStore.Intent.UpdateBottomPanelHeight -> {
                    val clampedHeight = min(0.7f, max(0.1f, intent.height))
                    dispatch(Msg.BottomPanelHeightUpdated(clampedHeight))
                }
                
                is CodeEditorStore.Intent.AddLogEntry -> {
                    dispatch(Msg.LogEntryAdded(intent.entry))
                }

                CodeEditorStore.Intent.OnCreateTestClick -> sendTestCreationPrompt(state().chatMessages)
            }
        }

        private fun sendTestCreationPrompt(
            currentMessages: List<UiChatMessage>,
        ) {
            val selectedFile = state().selectedFile
            val fileContent = selectedFile?.content.orEmpty()
            val testPrompt = """
                Create and run jUnit tests for this kotlin file:
                
                $fileContent
            """.trimIndent()
            val displayedMessage = "Create jUnit tests for ${state().selectedFile?.name}"

            val userMessage = UiChatMessage.createUserMessage(displayedMessage)
            val thinkingMessage = UiChatMessage.createThinkingMessage()

            // Add user message and thinking placeholder
            val updatedMessages = currentMessages + userMessage + thinkingMessage
            dispatch(Msg.MessagesUpdated(updatedMessages))
            dispatch(Msg.ChatInputUpdated(""))
            dispatch(Msg.LoadingChanged(true))

            scope.launch {
                try {
                    val apiMessages = prepareApiMessages(testPrompt, currentMessages)

                    val result = aiClient.createChatCompletion(
                        messages = apiMessages,
                    )

                    result.fold(
                        onSuccess = { responseContent ->
                           try {
                               val responseMessage = responseContent.message

                               if (responseMessage.contains("run_junit_tests")) {
                                   handleRunDockerWithCodeToolCall(
                                       message = responseMessage,
                                       filePath = selectedFile?.path.orEmpty(),
                                       currentMessages = currentMessages
                                   )
                               } else {
                                   throw IllegalStateException("tool_call was now existing in ai response, response - $responseMessage")
                               }
                           } catch (error: Exception) {
                               val errorMessages = currentMessages + userMessage
                               dispatch(Msg.MessagesUpdated(errorMessages))
                               dispatch(Msg.ErrorOccurred(error.message ?: "Unknown error occurred"))
                               dispatch(Msg.LoadingChanged(false))
                           }

                        },
                        onFailure = { error ->
                            // Remove thinking message and show error
                            val errorMessages = currentMessages + userMessage
                            dispatch(Msg.MessagesUpdated(errorMessages))
                            dispatch(Msg.ErrorOccurred(error.message ?: "Unknown error occurred"))
                            dispatch(Msg.LoadingChanged(false))
                        }
                    )
                } catch (e: Exception) {
                    // Remove thinking message and show error
                    val errorMessages = currentMessages + userMessage
                    dispatch(Msg.MessagesUpdated(errorMessages))
                    dispatch(Msg.ErrorOccurred(e.message ?: "Unknown error occurred"))
                    dispatch(Msg.LoadingChanged(false))
                }
            }
        }

        private suspend fun handleMcpToolCall(message: String) {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val toolCall = json.decodeFromString(ToolCall.serializer(), message)

            dispatch(Msg.LogEntryAdded("Agent call tool:\n$toolCall".createInfoLogEntry()))

            val result = githubMCPClient.callTool(toolCall)?.firstOrNull()?.text.orEmpty()
            dispatch(Msg.LogEntryAdded("result from tool ${toolCall.tool} - $result".createInfoLogEntry()))

            if (toolCall.tool == "generate_release_info") {
                dispatch(Msg.LogEntryAdded("!!Release info generated, nex stage - release creation!!".createInfoLogEntry()))
                sendAssistantMessage(
                    message = "Данные для создания релиза, продолжи пайплайн с этими данными - $result",
                    currentMessages = state().chatMessages
                )
            }
        }

        private fun sendAssistantMessage(
            message: String,
            currentMessages: List<UiChatMessage>,
        ) {
            scope.launch {
                try {
                    val apiMessages = prepareAssistantMessages(message, currentMessages)

                    val result = aiClient.createChatCompletion(
                        messages = apiMessages,
                    )

                    result.fold(
                        onSuccess = { responseContent ->
                            val responseMessage = responseContent.message

                            if (responseMessage.contains("call_tool")) {
                                handleMcpToolCall(message = responseMessage,)
                                return@fold
                            }

                        },
                        onFailure = { error -> error(error)}
                    )
                } catch (e: Exception) {
                    dispatch(Msg.LogEntryAdded("Error during sendAssistantMessage, ${e.message}".createInfoLogEntry()))
                }
            }
        }

        private suspend fun handleRunDockerWithCodeToolCall(
            message: String,
            filePath: String,
            currentMessages: List<UiChatMessage>
        ) {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val toolCall = json.decodeFromString(ToolCall.serializer(), message)
            val currentToolCall = toolCall.copy(
                params = buildMap {
                    putAll(toolCall.params)
                    replace("file_path", state().project.path + filePath)
                }
            )
            println("current tool call is - $currentToolCall")
            val toolLogEntry = LogEntry(
                message = "Tests created. Start process to check tests",
                level = UiLogLevel.INFO,
            )
            dispatch(Msg.LogEntryAdded(toolLogEntry))
            val assistantMessage2 = UiChatMessage.createAssistantMessage("Tests are created!!\nNext stage - create container for checking test")
            dispatch(Msg.MessagesUpdated(currentMessages + listOf(assistantMessage2)))


            val result: String = dockerMCPClient.callTool(currentToolCall)?.firstOrNull()?.text.orEmpty()

            println("mcpresult - $result")
            val logEntry = LogEntry(
                message = result,
                level = UiLogLevel.INFO,
            )

            val assistantMessage = UiChatMessage.createAssistantMessage(result.substringBeforeLast("Output"))
            fetchRootNode(filePath = state().project.path)
            dispatch(Msg.MessagesUpdated(currentMessages + listOf(assistantMessage)))
            dispatch(Msg.LoadingChanged(false))
            dispatch(Msg.LogEntryAdded(logEntry))
        }

        private fun sendMessage(
            message: String,
            currentMessages: List<UiChatMessage>,
        ) {
            val userMessage = UiChatMessage.createUserMessage(message)
            val thinkingMessage = UiChatMessage.createThinkingMessage()

            // Add user message and thinking placeholder
            val updatedMessages = currentMessages + userMessage + thinkingMessage
            dispatch(Msg.MessagesUpdated(updatedMessages))
            dispatch(Msg.ChatInputUpdated(""))
            dispatch(Msg.LoadingChanged(true))

            scope.launch {
                try {
                    val apiMessages = prepareApiMessages(message, currentMessages)

                    val result = aiClient.createChatCompletion(
                        messages = apiMessages,
                    )

                    result.fold(
                        onSuccess = { responseContent ->
                            val responseMessage = responseContent.message

                            if (responseMessage.contains("call_tool")) {
                                handleMcpToolCall(message = responseMessage,)
                                return@fold
                            }

                            val finalMessages = (currentMessages + userMessage + UiChatMessage.createAssistantMessage(responseMessage))

                            dispatch(Msg.MessagesUpdated(finalMessages))
                            dispatch(Msg.LoadingChanged(false))
                        },
                        onFailure = { error ->
                            // Remove thinking message and show error
                            val errorMessages = currentMessages + userMessage
                            dispatch(Msg.MessagesUpdated(errorMessages))
                            dispatch(Msg.ErrorOccurred(error.message ?: "Unknown error occurred"))
                            dispatch(Msg.LoadingChanged(false))
                        }
                    )
                } catch (e: Exception) {
                    // Remove thinking message and show error
                    val errorMessages = currentMessages + userMessage
                    dispatch(Msg.MessagesUpdated(errorMessages))
                    dispatch(Msg.ErrorOccurred(e.message ?: "Unknown error occurred"))
                    dispatch(Msg.LoadingChanged(false))
                }
            }
        }

        private suspend fun prepareApiMessages(
            message: String,
            currentMessages: List<UiChatMessage>,
        ): List<ChatMessage> {
            val tools = githubMCPClient.getTools()
            val systemPrompt = generateSystemPrompt(tools)
            val systemMessage = ChatMessage(ChatMessage.ROLE_SYSTEM, systemPrompt)
            val newMessage = ChatMessage(role = ChatMessage.ROLE_USER, content = message)

            return listOf(systemMessage) + (currentMessages.map { it.toChatMessage() } + newMessage)
        }

        private suspend fun prepareAssistantMessages(
            message: String,
            currentMessages: List<UiChatMessage>,
        ): List<ChatMessage> {
            val tools = githubMCPClient.getTools()
            val systemPrompt = generateSystemPrompt(tools)
            val systemMessage = ChatMessage(ChatMessage.ROLE_SYSTEM, systemPrompt)
            val newMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = message)

            return listOf(systemMessage) + (currentMessages.map { it.toChatMessage() } + newMessage)
        }

        private fun generateSystemPrompt(tools: List<Tool>): String {
            return """
                Ты — релиз-менеджер, управляющий выпуском релизов через предоставленные тебе инструменты.
                
                Список инструментов:
                    $tools
                
                При запросе пользователя на создание релиза ты обязан действовать строго по пайплайну:
                (Команду на создание пайплайна нужно обработать только 1 раз, не нужно каждый раз вызывать generate_release_info, это должно произойти только 1 раз)
                
                Сначала вызови команду generate_release_info.
                
                Она возвращает следующую версию и changelog (список коммитов) сообщением с ролью assistant. Никогда не придумывай данные самостоятельно.
                
                На основе списка коммитов из changelog создай человеко-ориентированные release notes:
                
                Используй понятный язык для конечных пользователей.
                
                Убирай технические детали (например, chore:, refactor:, SHA коммитов).
                
                Группируй изменения по категориям («Новые возможности», «Исправления», «Улучшения»), если это уместно.
                
                Затем вызови команду create_release. При вызове команды create_release отвечай только обусловленным JSON, больше никакй информации или форматирования MArkdown быть не должно, ПРИДЕРЖИВАЙСЯ ИНСТРУКЦИЯМ
                
                В качестве версии используй значение version из результата generate_release_info.
                
                В качестве описания используй подготовленные тобой release notes.
                
                Правила:
                
                Никогда не пропускай шаг преобразования changelog в человеко-ориентированные release notes.
                
                Никогда не передавай список коммитов напрямую в релиз.
                
                Если любая команда завершилась ошибкой — остановись и сообщи пользователю. 
                Все вызовы инструментов делай только в формате JSON:
                
                    {
                    "action": "call_tool",
                    "tool": "<tool_name>",
                    "params": { <key-value pairs> }
                    }
            """.trimIndent()
        }


        private fun generateTestSystemPrompt(tools: List<Tool>): String {
            return """
You are an AI agent integrated with a mcp server.  
You must follow these rules strictly:

1. You are given a list of tools:
    $tools

2. When the user makes a request that can be satisfied by one of the tools,  
   you must respond **only** with a JSON object of the following form:

   {
     "action": "call_tool",
     "tool": "<tool_name>",
     "params": { <key-value pairs> }
   }

3. Never include explanations, natural language, comments, or any additional text.  
   Your output must always be a **valid JSON object only**, parsable by standard JSON parsers.  

4. If the request cannot be mapped to any available tool, respond with:

   {
     "action": "no_tool",
     "reason": "<short machine-readable reason>"
   }

5. Do not invent tools, parameters, or functionality.  
   Only use the provided tool list.  

6. Preserve the integrity of code and text passed as parameters (do not escape or reformat unnecessarily).  

This system prompt establishes a strict contract:  
You are a JSON-only tool invocation layer.  
No additional conversation, no reasoning, no markdown formatting.  
            """.trimIndent()
        }
    }

    private object ReducerImpl : Reducer<CodeEditorStore.State, Msg> {
        override fun CodeEditorStore.State.reduce(msg: Msg): CodeEditorStore.State =
            when (msg) {
                is Msg.FileSelected -> copy(selectedFile = msg.file)
                is Msg.EditorContentUpdated -> copy(editorContent = msg.content)
                is Msg.ChatInputUpdated -> copy(chatInput = msg.input)
                is Msg.ChatMessageAdded -> copy(chatMessages = chatMessages + msg.message)
                is Msg.PanelWidthUpdated -> when (msg.uiPanel) {
                    UiPanel.LEFT -> copy(leftPanelWidth = msg.width)
                    UiPanel.RIGHT -> copy(rightPanelWidth = msg.width)
                }
                is Msg.BottomPanelHeightUpdated -> copy(bottomPanelHeight = msg.height)
                is Msg.LogEntryAdded -> copy(logs = logs + msg.entry)
                is Msg.OnNodesReceived -> copy(projectTree = msg.node.children)
                is Msg.ProjectTreeUpdated -> copy(projectTree = msg.tree)
                is Msg.MessagesUpdated -> copy(chatMessages = msg.messages)
                is Msg.LoadingChanged -> copy(isLoading = msg.isLoading)
                is Msg.ErrorOccurred -> copy(error = msg.error, isLoading = false)
                is Msg.ErrorCleared -> copy(error = null)
            }
    }
}
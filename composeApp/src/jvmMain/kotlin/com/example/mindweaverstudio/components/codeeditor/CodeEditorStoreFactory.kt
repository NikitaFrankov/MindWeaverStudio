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
import com.example.mindweaverstudio.components.codeeditor.utils.scanDirectoryToFileNode
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.data.agents.orchestrator.AgentsOrchestratorFactory
import com.example.mindweaverstudio.data.mcp.DockerMCPClient
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStore.Msg
import com.example.mindweaverstudio.data.receivers.CodeEditorLogReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlin.collections.plus
import kotlin.math.max
import kotlin.math.min

class CodeEditorStoreFactory(
    orchestratorFactory: AgentsOrchestratorFactory,
    private val logReceiver: CodeEditorLogReceiver,
    private val dockerMCPClient: DockerMCPClient,
    private val githubMCPClient: GithubMCPClient,
    private val storeFactory: StoreFactory,
) {

    private val orchestrator = orchestratorFactory.editorOrchestrator

    fun create(project: Project): CodeEditorStore =
        object : CodeEditorStore, Store<CodeEditorStore.Intent, CodeEditorStore.State, CodeEditorStore.Label> by storeFactory.create(
            name = "CodeEditorStore",
            initialState = CodeEditorStore.State(project = project),
            bootstrapper = SimpleBootstrapper(CodeEditorStore.Action.Init),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<CodeEditorStore.Intent, CodeEditorStore.Action, CodeEditorStore.State, Msg, CodeEditorStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        override fun executeAction(action: CodeEditorStore.Action) = when(action) {
            CodeEditorStore.Action.Init -> {
                initMcpServer()
                fetchRootNode(state().project.path)
                setupLogListener()
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

        private fun setupLogListener() {
            scope.launch {
                logReceiver.logFlow.collect { logEntry ->
                    dispatch(Msg.LogEntryAdded(logEntry))
                }
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
            }
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
                    val result = orchestrator.handleMessage(message)

                    if (result.isError) {
                        dispatch(Msg.ErrorOccurred(result.message))
                    }
                    val finalMessages = currentMessages + listOf(UiChatMessage.createAssistantMessage(result.message))

                    dispatch(Msg.MessagesUpdated(finalMessages))
                    dispatch(Msg.LoadingChanged(false))
                } catch (e: Exception) {
                    // Remove thinking message and show error
                    val errorMessages = currentMessages + userMessage
                    dispatch(Msg.MessagesUpdated(errorMessages))
                    dispatch(Msg.ErrorOccurred(e.message ?: "Unknown error occurred"))
                    dispatch(Msg.LoadingChanged(false))
                }
            }
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
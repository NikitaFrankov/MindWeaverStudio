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
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStore.Msg
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStore.Msg.*
import com.example.mindweaverstudio.data.ai.orchestrator.code.CodeOrchestrator
import com.example.mindweaverstudio.data.receivers.CodeEditorLogReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlin.collections.plus
import kotlin.math.max
import kotlin.math.min

class CodeEditorStoreFactory(
    private val orchestrator: CodeOrchestrator,
    private val logReceiver: CodeEditorLogReceiver,
    private val storeFactory: StoreFactory,
) {

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
                fetchRootNode(state().project.path)
                setupLogListener()
                checkRepositoryInfo()
            }
        }

        private fun fetchRootNode(filePath: String) {
            scope.launch {
                val node = scanDirectoryToFileNode(rootPathStr = filePath)
                dispatch(OnNodesReceived(node))
            }
        }

        private fun setupLogListener() {
            scope.launch {
                logReceiver.logFlow.collect { logEntry ->
                    dispatch(LogEntryAdded(logEntry))
                }
            }
        }

        private fun checkRepositoryInfo() {
            scope.launch {
                delay(1000L)
                publish(CodeEditorStore.Label.ShowGithubInfoInputDialog)
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
                        dispatch(FileSelected(intent.file))
                        dispatch(EditorContentUpdated(intent.file.content.orEmpty()))
                        dispatch(
                            LogEntryAdded(
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
                    dispatch(ProjectTreeUpdated(updatedTree))
                }
                
                is CodeEditorStore.Intent.UpdateEditorContent -> {
                    dispatch(EditorContentUpdated(intent.content))
                }
                
                is CodeEditorStore.Intent.UpdateChatInput -> {
                    dispatch(ChatInputUpdated(intent.input))
                }
                
                is CodeEditorStore.Intent.SendChatMessage -> {
                    val currentState = state()
                    if (currentState.chatInput.isNotBlank() && !currentState.isLoading) {
                        sendMessage(currentState.chatInput, currentState.chatMessages)
                    }
                }
                
                is CodeEditorStore.Intent.ClearError -> dispatch(ErrorCleared)

                is CodeEditorStore.Intent.UpdatePanelWidth -> {
                    val clampedWidth = min(0.8f, max(0.1f, intent.width))
                    dispatch(PanelWidthUpdated(intent.uiPanel, clampedWidth))
                }
                
                is CodeEditorStore.Intent.UpdateBottomPanelHeight -> {
                    val clampedHeight = min(0.7f, max(0.1f, intent.height))
                    dispatch(BottomPanelHeightUpdated(clampedHeight))
                }
                
                is CodeEditorStore.Intent.AddLogEntry -> {
                    dispatch(LogEntryAdded(intent.entry))
                }

                CodeEditorStore.Intent.RecordVoiceClick -> {

                }

                is CodeEditorStore.Intent.PlayMessage -> playMessage(intent.message)
            }
        }

        private fun playMessage(message: String) {
            ProcessBuilder("say", message).start()
        }

        private fun sendMessage(
            message: String,
            currentMessages: List<UiChatMessage>,
        ) {
            val userMessage = UiChatMessage.createUserMessage(message)
            val thinkingMessage = UiChatMessage.createThinkingMessage()

            val updatedMessages = currentMessages + userMessage + thinkingMessage
            dispatch(MessagesUpdated(updatedMessages))
            dispatch(ChatInputUpdated(""))
            dispatch(LoadingChanged(true))

            scope.launch(Dispatchers.IO) {
                try {
                    val result = orchestrator.run(message)

//                    if (result.isError) {
//                        dispatch(ErrorOccurred(result.message))
//                    }
                    val finalMessages = currentMessages + userMessage + listOf(UiChatMessage.createAssistantMessage(result))

                    withContext(Dispatchers.Main) {
                        dispatch(MessagesUpdated(finalMessages))
                        dispatch(LoadingChanged(false))
                    }
                } catch (e: Exception) {
                    val errorMessages = currentMessages + userMessage
                    withContext(Dispatchers.Main) {
                        dispatch(MessagesUpdated(errorMessages))
                        dispatch(ErrorOccurred(e.message ?: "Unknown error occurred"))
                        dispatch(LoadingChanged(false))
                    }
                }
            }
        }
    }

    private object ReducerImpl : Reducer<CodeEditorStore.State, Msg> {
        override fun CodeEditorStore.State.reduce(msg: Msg): CodeEditorStore.State =
            when (msg) {
                is FileSelected -> copy(selectedFile = msg.file)
                is EditorContentUpdated -> copy(editorContent = msg.content)
                is ChatInputUpdated -> copy(chatInput = msg.input)
                is ChatMessageAdded -> copy(chatMessages = chatMessages + msg.message)
                is PanelWidthUpdated -> when (msg.uiPanel) {
                    UiPanel.LEFT -> copy(leftPanelWidth = msg.width)
                    UiPanel.RIGHT -> copy(rightPanelWidth = msg.width)
                }
                is BottomPanelHeightUpdated -> copy(bottomPanelHeight = msg.height)
                is LogEntryAdded -> copy(logs = logs + msg.entry)
                is OnNodesReceived -> copy(projectTree = msg.node.children)
                is ProjectTreeUpdated -> copy(projectTree = msg.tree)
                is MessagesUpdated -> copy(chatMessages = msg.messages)
                is LoadingChanged -> copy(isLoading = msg.isLoading)
                is ErrorOccurred -> copy(error = msg.error, isLoading = false)
                is ErrorCleared -> copy(error = null)
                is VoiceRecordingStateChange -> copy(isVoiceRecording = !isVoiceRecording)
            }
    }
}
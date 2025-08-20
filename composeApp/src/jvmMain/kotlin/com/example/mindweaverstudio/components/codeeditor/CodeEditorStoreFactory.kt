package com.example.mindweaverstudio.components.codeeditor

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.components.codeeditor.models.ChatMessage
import com.example.mindweaverstudio.components.codeeditor.models.FileNode
import com.example.mindweaverstudio.components.codeeditor.models.LogEntry
import com.example.mindweaverstudio.components.codeeditor.models.LogLevel
import com.example.mindweaverstudio.components.codeeditor.models.Panel
import com.example.mindweaverstudio.components.codeeditor.utils.scanDirectoryToFileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlin.math.max
import kotlin.math.min

class CodeEditorStoreFactory(
    private val storeFactory: StoreFactory
) {

    private val filePath: String = "/Users/nikitaradionov/IdeaProjects/SimpleProject"

    fun create(): CodeEditorStore =
        object : CodeEditorStore, Store<CodeEditorStore.Intent, CodeEditorStore.State, CodeEditorStore.Label> by storeFactory.create(
            name = "CodeEditorStore",
            initialState = CodeEditorStore.State(),
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
        class ChatMessageAdded(val message: ChatMessage) : Msg()
        class PanelWidthUpdated(val panel: Panel, val width: Float) : Msg()
        class BottomPanelHeightUpdated(val height: Float) : Msg()
        class LogEntryAdded(val entry: LogEntry) : Msg()
        class OnNodesReceived(val node: FileNode) : Msg()
    }

    private inner class ExecutorImpl : CoroutineExecutor<CodeEditorStore.Intent, Action, CodeEditorStore.State, Msg, CodeEditorStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        override fun executeAction(action: Action) = when(action) {
            Action.Init -> fetchRootNode(filePath)
        }

        private fun fetchRootNode(filePath: String) {
            scope.launch {
                val node = scanDirectoryToFileNode(rootPathStr = filePath)
                dispatch(Msg.OnNodesReceived(node))
            }
        }

        override fun executeIntent(intent: CodeEditorStore.Intent) {
            when (intent) {
                is CodeEditorStore.Intent.SelectFile -> {
                    if (!intent.file.isDirectory) {
                        dispatch(Msg.FileSelected(intent.file))
                        dispatch(Msg.EditorContentUpdated(intent.file.content.orEmpty()))
                        dispatch(Msg.LogEntryAdded(
                            LogEntry(
                                "Opened file: ${intent.file.name}",
                                LogLevel.INFO
                            )
                        ))
                    }
                }
                
                is CodeEditorStore.Intent.UpdateEditorContent -> {
                    dispatch(Msg.EditorContentUpdated(intent.content))
                }
                
                is CodeEditorStore.Intent.UpdateChatInput -> {
                    dispatch(Msg.ChatInputUpdated(intent.input))
                }
                
                is CodeEditorStore.Intent.SendChatMessage -> Unit

                is CodeEditorStore.Intent.UpdatePanelWidth -> {
                    val clampedWidth = min(0.8f, max(0.1f, intent.width))
                    dispatch(Msg.PanelWidthUpdated(intent.panel, clampedWidth))
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
    }

    private object ReducerImpl : Reducer<CodeEditorStore.State, Msg> {
        override fun CodeEditorStore.State.reduce(msg: Msg): CodeEditorStore.State =
            when (msg) {
                is Msg.FileSelected -> copy(selectedFile = msg.file)
                is Msg.EditorContentUpdated -> copy(editorContent = msg.content)
                is Msg.ChatInputUpdated -> copy(chatInput = msg.input)
                is Msg.ChatMessageAdded -> copy(chatMessages = chatMessages + msg.message)
                is Msg.PanelWidthUpdated -> when (msg.panel) {
                    Panel.LEFT -> copy(leftPanelWidth = msg.width)
                    Panel.RIGHT -> copy(rightPanelWidth = msg.width)
                }
                is Msg.BottomPanelHeightUpdated -> copy(bottomPanelHeight = msg.height)
                is Msg.LogEntryAdded -> copy(logs = logs + msg.entry)
                is Msg.OnNodesReceived -> copy(projectTree = msg.node.children)
            }
    }
}
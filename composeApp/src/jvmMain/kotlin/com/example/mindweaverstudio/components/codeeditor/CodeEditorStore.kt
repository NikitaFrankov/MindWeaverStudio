package com.example.mindweaverstudio.components.codeeditor

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.components.codeeditor.models.FileNode
import com.example.mindweaverstudio.components.codeeditor.models.LogEntry
import com.example.mindweaverstudio.components.codeeditor.models.UiChatMessage
import com.example.mindweaverstudio.components.codeeditor.models.UiPanel
import com.example.mindweaverstudio.components.projectselection.Project

interface CodeEditorStore : Store<CodeEditorStore.Intent, CodeEditorStore.State, CodeEditorStore.Label> {

    data class State(
        val project: Project,
        val projectTree: List<FileNode> = emptyList(),
        val selectedFile: FileNode? = null,
        val editorContent: String = "",
        val chatMessages: List<UiChatMessage> = emptyList(),
        val chatInput: String = "",
        val logs: List<LogEntry> = emptyList(),
        val leftPanelWidth: Float = 0.2f,
        val rightPanelWidth: Float = 0.3f,
        val bottomPanelHeight: Float = 0.3f,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isVoiceRecording: Boolean = false,
    )

    sealed class Intent {
        class SelectFile(val file: FileNode) : Intent()
        class ToggleFolderExpanded(val folderPath: String) : Intent()
        class UpdateEditorContent(val content: String) : Intent()
        class UpdateChatInput(val input: String) : Intent()
        class PlayMessage(val message: String) : Intent()
        data object SendChatMessage : Intent()
        data class UpdatePanelWidth(val uiPanel: UiPanel, val width: Float) : Intent()
        data class UpdateBottomPanelHeight(val height: Float) : Intent()
        data class AddLogEntry(val entry: LogEntry) : Intent()
        data object ClearError : Intent()
        data object RecordVoiceClick : Intent()
    }

    sealed class Label

    sealed interface Action {
        data object Init : Action
    }

    sealed class Msg {
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
        data object VoiceRecordingStateChange : Msg()
    }
}
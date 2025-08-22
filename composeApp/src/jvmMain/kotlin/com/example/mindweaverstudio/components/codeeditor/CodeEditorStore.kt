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
        val error: String? = null
    )

    sealed class Intent {
        data class SelectFile(val file: FileNode) : Intent()
        data class UpdateEditorContent(val content: String) : Intent()
        data class UpdateChatInput(val input: String) : Intent()
        data object SendChatMessage : Intent()
        data class UpdatePanelWidth(val uiPanel: UiPanel, val width: Float) : Intent()
        data class UpdateBottomPanelHeight(val height: Float) : Intent()
        data class AddLogEntry(val entry: LogEntry) : Intent()
        data object ClearError : Intent()
        data object OnCreateTestClick : Intent()
    }

    sealed class Label {
        data class ShowError(val message: String) : Label()
    }

}
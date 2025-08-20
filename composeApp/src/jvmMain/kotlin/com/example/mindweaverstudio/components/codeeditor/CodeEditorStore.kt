package com.example.mindweaverstudio.components.codeeditor

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.components.codeeditor.models.ChatMessage
import com.example.mindweaverstudio.components.codeeditor.models.FileNode
import com.example.mindweaverstudio.components.codeeditor.models.LogEntry
import com.example.mindweaverstudio.components.codeeditor.models.Panel

interface CodeEditorStore : Store<CodeEditorStore.Intent, CodeEditorStore.State, CodeEditorStore.Label> {

    data class State(
        val projectTree: List<FileNode> = emptyList(),
        val selectedFile: FileNode? = null,
        val editorContent: String = "",
        val chatMessages: List<ChatMessage> = emptyList(),
        val chatInput: String = "",
        val logs: List<LogEntry> = emptyList(),
        val leftPanelWidth: Float = 0.2f,
        val rightPanelWidth: Float = 0.3f,
        val bottomPanelHeight: Float = 0.3f
    )

    sealed class Intent {
        data class SelectFile(val file: FileNode) : Intent()
        data class UpdateEditorContent(val content: String) : Intent()
        data class UpdateChatInput(val input: String) : Intent()
        data object SendChatMessage : Intent()
        data class UpdatePanelWidth(val panel: Panel, val width: Float) : Intent()
        data class UpdateBottomPanelHeight(val height: Float) : Intent()
        data class AddLogEntry(val entry: LogEntry) : Intent()
    }

    sealed class Label {
        data class ShowError(val message: String) : Label()
    }

}
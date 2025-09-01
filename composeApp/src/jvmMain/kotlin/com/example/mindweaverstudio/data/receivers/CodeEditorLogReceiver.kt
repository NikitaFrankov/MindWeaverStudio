package com.example.mindweaverstudio.data.receivers

import com.example.mindweaverstudio.components.codeeditor.models.LogEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CodeEditorLogReceiver {
    private var _logFlow: MutableSharedFlow<LogEntry> = MutableSharedFlow()
    val logFlow: SharedFlow<LogEntry> = _logFlow.asSharedFlow()

    suspend fun emitNewValue(value: LogEntry) {
        _logFlow.emit(value)
    }
}
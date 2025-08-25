package com.example.mindweaverstudio.components.codeeditor.models

import kotlin.String

class LogEntry(
    val message: String,
    val level: UiLogLevel,
    val timestamp: Long = System.currentTimeMillis()
)

fun String.createInfoLogEntry() = LogEntry(
    level = UiLogLevel.INFO,
    message = this,
)
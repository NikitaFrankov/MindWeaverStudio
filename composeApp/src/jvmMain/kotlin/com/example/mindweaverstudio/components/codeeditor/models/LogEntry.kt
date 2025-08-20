package com.example.mindweaverstudio.components.codeeditor.models

data class LogEntry(
    val message: String,
    val level: LogLevel,
    val timestamp: Long = System.currentTimeMillis()
)
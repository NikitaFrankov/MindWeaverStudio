package com.example.mindweaverstudio.data.models.mcp.base

import kotlinx.serialization.Serializable

@Serializable
data class ToolCall(
    val action: String,
    val tool: String,
    val params: Map<String, String> = emptyMap(),
    val isError: Boolean = false,
    val errorMessage: String? = null,
)
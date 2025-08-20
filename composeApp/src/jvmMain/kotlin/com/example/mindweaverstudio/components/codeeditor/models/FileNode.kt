package com.example.mindweaverstudio.components.codeeditor.models

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val content: String? = null,
    val children: List<FileNode> = emptyList()
)
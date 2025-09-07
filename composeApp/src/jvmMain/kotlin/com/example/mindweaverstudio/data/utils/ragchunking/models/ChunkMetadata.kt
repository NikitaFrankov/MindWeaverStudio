package com.example.mindweaverstudio.data.utils.ragchunking.models

import kotlinx.serialization.Serializable

@Serializable
data class ChunkMetadata(
    val filePath: String,
    val className: String? = null,
    val methodName: String? = null,
    val startLine: Int,
    val endLine: Int,
    val content: String,
    val chunkType: ChunkType,
    val tokens: Int,
    val overlapsWithPrevious: Boolean = false,
    val contextualInfo: ContextualInfo
)

@Serializable
data class ContextualInfo(
    val packageDeclaration: String? = null,
    val imports: List<String> = emptyList(),
    val classSignature: String? = null,
    val relatedPrivateMethods: List<String> = emptyList()
)

@Serializable
enum class ChunkType {
    CLASS,
    INTERFACE,
    ENUM,
    METHOD,
    FUNCTION,
    PROPERTY,
    SUB_CHUNK
}

@Serializable
data class CodeElement(
    val type: ChunkType,
    val name: String,
    val startLine: Int,
    val endLine: Int,
    val signature: String,
    val modifiers: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val documentation: String? = null
)

@Serializable
data class FileAnalysis(
    val filePath: String,
    val packageDeclaration: String?,
    val imports: List<String>,
    val elements: List<CodeElement>,
    val totalLines: Int
)

data class LogicalBlock(
    val startLine: Int,
    val endLine: Int,
    val type: BlockType
)

enum class BlockType {
    CONDITIONAL, LOOP, EXCEPTION, WHEN, SEQUENTIAL
}
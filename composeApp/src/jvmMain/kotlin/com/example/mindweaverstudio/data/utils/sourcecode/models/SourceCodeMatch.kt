package com.example.mindweaverstudio.data.utils.sourcecode.models

data class SourceCodeMatch(
    val entityName: String,
    val entityType: EntityType,
    val sourceCode: String,
    val filePath: String,
    val packageName: String?,
    val lineNumber: Int,
    val imports: List<String>
)

enum class EntityType {
    CLASS,
    INTERFACE,
    OBJECT,
    DATA_CLASS,
    SEALED_CLASS,
    ENUM_CLASS,
    FUNCTION,
    CONSTANT,
    PROPERTY,
    ANNOTATION_CLASS
}
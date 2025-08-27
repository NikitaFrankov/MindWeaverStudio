package com.example.mindweaverstudio.data.utils.sourcecode

import com.example.mindweaverstudio.data.utils.sourcecode.models.SearchResult
import com.example.mindweaverstudio.data.utils.sourcecode.models.SourceCodeMatch

class SourceCodeFormatter {
    
    /**
     * Formats the search result into a human-readable string.
     */
    fun formatResult(result: SearchResult): String {
        return when (result) {
            is SearchResult.Success -> formatSingleMatch(result.matches.first())
            is SearchResult.MultipleMatches -> formatMultipleMatches(result.matches)
            is SearchResult.NotFound -> formatNotFound(result.targetName, result.searchedFiles)
            is SearchResult.Error -> formatError(result.message, result.cause)
        }
    }
    
    /**
     * Formats a single source code match with imports.
     */
    private fun formatSingleMatch(match: SourceCodeMatch): String {
        val imports = if (match.imports.isNotEmpty()) {
            match.imports.joinToString("\n") { "import $it" } + "\n\n"
        } else ""
        
        val packageDeclaration = if (match.packageName != null) {
            "package ${match.packageName}\n\n"
        } else ""
        
        return buildString {
            appendLine("// Found: ${match.entityName} (${match.entityType.name.lowercase()})")
            appendLine("// File: ${match.filePath}")
            appendLine("// Line: ${match.lineNumber}")
            if (match.packageName != null) {
                appendLine("// Package: ${match.packageName}")
            }
            appendLine()
            append(packageDeclaration)
            append(imports)
            append(match.sourceCode)
        }
    }
    
    /**
     * Formats multiple matches with disambiguation information.
     */
    private fun formatMultipleMatches(matches: List<SourceCodeMatch>): String {
        return buildString {
            appendLine("Multiple matches found for '${matches.first().entityName}':")
            appendLine()
            
            matches.forEachIndexed { index, match ->
                appendLine("--- Match ${index + 1} ---")
                appendLine("Type: ${match.entityType.name.lowercase()}")
                appendLine("Package: ${match.packageName ?: "default"}")
                appendLine("File: ${match.filePath}")
                appendLine("Line: ${match.lineNumber}")
                appendLine()
                
                val imports = if (match.imports.isNotEmpty()) {
                    match.imports.joinToString("\n") { "import $it" } + "\n\n"
                } else ""
                
                val packageDeclaration = if (match.packageName != null) {
                    "package ${match.packageName}\n\n"
                } else ""
                
                append(packageDeclaration)
                append(imports)
                append(match.sourceCode)
                appendLine()
                appendLine()
            }
        }
    }
    
    /**
     * Formats not found result.
     */
    private fun formatNotFound(targetName: String, searchedFiles: Int): String {
        return "Entity '$targetName' not found in $searchedFiles Kotlin files."
    }
    
    /**
     * Formats error result.
     */
    private fun formatError(message: String, cause: Throwable?): String {
        return buildString {
            appendLine("Error during search: $message")
            if (cause != null) {
                appendLine("Cause: ${cause.message}")
            }
        }
    }
    
    /**
     * Gets source code with imports only (no metadata comments).
     */
    fun getCleanSourceCode(match: SourceCodeMatch): String {
        val imports = if (match.imports.isNotEmpty()) {
            match.imports.joinToString("\n") { "import $it" } + "\n\n"
        } else ""
        
        val packageDeclaration = if (match.packageName != null) {
            "package ${match.packageName}\n\n"
        } else ""
        
        return packageDeclaration + imports + match.sourceCode
    }
}
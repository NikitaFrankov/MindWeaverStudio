package com.example.mindweaverstudio.data.utils.codereplacer.models

/**
 * Result of a code replacement operation.
 */
sealed class ReplacementResult {
    /**
     * Successful replacement.
     */
    data class Success(
        val filePath: String,
        val matchesFound: Int,
        val matchesReplaced: Int,
        val backupPath: String? = null,
        val message: String
    ) : ReplacementResult()
    
    /**
     * Code fragment not found in the file.
     */
    data class NotFound(
        val filePath: String,
        val originalCode: String,
        val searchedLines: Int
    ) : ReplacementResult()
    
    /**
     * Multiple matches found but allowMultipleMatches is false.
     */
    data class MultipleMatches(
        val filePath: String,
        val matches: List<ReplacementMatch>,
        val message: String
    ) : ReplacementResult()
    
    /**
     * Error during replacement operation.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val filePath: String? = null
    ) : ReplacementResult()
    
    /**
     * Dry run result - shows what would happen without making changes.
     */
    data class DryRun(
        val filePath: String,
        val matchesFound: Int,
        val matches: List<ReplacementMatch>,
        val previewContent: String,
        val message: String
    ) : ReplacementResult()
    
    /**
     * No changes were made (originalCode == newCode and allowNoChange is true).
     */
    data class NoChange(
        val filePath: String,
        val message: String
    ) : ReplacementResult()
}

/**
 * Information about a found code match.
 */
data class ReplacementMatch(
    val startLine: Int,                    // Line number where match starts (1-based)
    val endLine: Int,                      // Line number where match ends (1-based)
    val startColumn: Int,                  // Column where match starts (1-based)
    val endColumn: Int,                    // Column where match ends (1-based)
    val matchedText: String,               // The actual matched text
    val contextBefore: String,             // Lines before the match for context
    val contextAfter: String               // Lines after the match for context
) {
    /**
     * Gets a human-readable location description.
     */
    fun getLocationDescription(): String {
        return if (startLine == endLine) {
            "line $startLine, columns $startColumn-$endColumn"
        } else {
            "lines $startLine-$endLine"
        }
    }
}

/**
 * Extension functions for ReplacementResult.
 */
fun ReplacementResult.isSuccess(): Boolean = this is ReplacementResult.Success
fun ReplacementResult.isError(): Boolean = this is ReplacementResult.Error
fun ReplacementResult.getErrorMessage(): String? = when (this) {
    is ReplacementResult.Error -> message
    is ReplacementResult.NotFound -> "Code fragment not found in file $filePath"
    is ReplacementResult.MultipleMatches -> "Multiple matches found: $message"
    else -> null
}

fun ReplacementResult.getSuccessMessage(): String? = when (this) {
    is ReplacementResult.Success -> message
    is ReplacementResult.DryRun -> message
    is ReplacementResult.NoChange -> message
    else -> null
}
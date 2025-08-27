package com.example.mindweaverstudio.data.utils.codereplacer.models

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Represents a request to replace code in a file.
 */
data class ReplacementRequest(
    val filePath: String,
    val originalCode: String,
    val newCode: String,
    val options: ReplacementOptions = ReplacementOptions()
) {
    /**
     * Gets the file path as a Path object.
     */
    fun getPath(): Path = Paths.get(filePath)
    
    /**
     * Validates the request parameters.
     */
    fun validate(): ReplacementValidation {
        if (filePath.isBlank()) {
            return ReplacementValidation.Invalid("File path cannot be blank")
        }
        
        if (originalCode.isBlank()) {
            return ReplacementValidation.Invalid("Original code cannot be blank")
        }
        
        if (originalCode == newCode && !options.allowNoChange) {
            return ReplacementValidation.Invalid("Original and new code are identical")
        }
        
        return ReplacementValidation.Valid
    }
}

/**
 * Validation result for replacement requests.
 */
sealed class ReplacementValidation {
    object Valid : ReplacementValidation()
    data class Invalid(val message: String) : ReplacementValidation()
}

/**
 * Options for code replacement operations.
 */
data class ReplacementOptions(
    val exactMatch: Boolean = true,                    // Require exact string match
    val preserveWhitespace: Boolean = true,            // Preserve original whitespace
    val normalizeLineEndings: Boolean = true,          // Normalize line endings
    val allowMultipleMatches: Boolean = false,         // Allow multiple occurrences to be replaced
    val allowNoChange: Boolean = false,                // Allow originalCode == newCode
    val createBackup: Boolean = false,                 // Create backup file before modification
    val validateSyntax: Boolean = false,               // Validate Kotlin syntax after replacement
    val dryRun: Boolean = false                        // Only simulate replacement, don't write
)
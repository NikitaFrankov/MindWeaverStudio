package com.example.mindweaverstudio.data.utils.codereplacer

import com.example.mindweaverstudio.data.utils.codereplacer.models.*

/**
 * Utility functions for replacing code fragments in source files.
 * Provides a convenient API for the CodeReplacer functionality.
 */
object CodeReplacerUtils {
    
    private val codeReplacer = CodeReplacer()
    
    /**
     * Replaces a code fragment in a file with new code.
     * 
     * @param filePath Path to the source file
     * @param originalCode Original code fragment to replace
     * @param newCode New code to replace with
     * @return String message indicating success or failure
     */
    suspend fun replaceCodeInFile(
        filePath: String,
        originalCode: String,
        newCode: String
    ): String {
        val request = ReplacementRequest(filePath, originalCode, newCode)
        val result = codeReplacer.replaceCode(request)
        return formatResult(result)
    }
    
    /**
     * Replaces a code fragment with options for customization.
     * 
     * @param filePath Path to the source file
     * @param originalCode Original code fragment to replace
     * @param newCode New code to replace with
     * @param createBackup Whether to create a backup file
     * @param allowMultiple Whether to allow multiple matches to be replaced
     * @return String message indicating success or failure
     */
    suspend fun replaceCodeWithOptions(
        filePath: String,
        originalCode: String,
        newCode: String,
        createBackup: Boolean = false,
        allowMultiple: Boolean = false
    ): String {
        val options = ReplacementOptions(
            createBackup = createBackup,
            allowMultipleMatches = allowMultiple
        )
        val request = ReplacementRequest(filePath, originalCode, newCode, options)
        val result = codeReplacer.replaceCode(request)
        return formatResult(result)
    }
    
    /**
     * Performs a dry run to see what would be replaced without making changes.
     * 
     * @param filePath Path to the source file
     * @param originalCode Original code fragment to find
     * @param newCode New code that would replace it
     * @return String message showing what would be changed
     */
    suspend fun previewReplacement(
        filePath: String,
        originalCode: String,
        newCode: String
    ): String {
        val options = ReplacementOptions(dryRun = true)
        val request = ReplacementRequest(filePath, originalCode, newCode, options)
        val result = codeReplacer.replaceCode(request)
        return formatResult(result)
    }
    
    /**
     * Safely replaces code with comprehensive options.
     * 
     * @param filePath Path to the source file
     * @param originalCode Original code fragment to replace
     * @param newCode New code to replace with
     * @param options Replacement options for customization
     * @return ReplacementResult with detailed information
     */
    suspend fun replaceCodeSafely(
        filePath: String,
        originalCode: String,
        newCode: String,
        options: ReplacementOptions = ReplacementOptions()
    ): ReplacementResult {
        val request = ReplacementRequest(filePath, originalCode, newCode, options)
        return codeReplacer.replaceCode(request)
    }
    
    /**
     * Checks if a code fragment exists in a file without replacing it.
     * 
     * @param filePath Path to the source file
     * @param codeFragment Code fragment to search for
     * @return String indicating whether the code was found
     */
    suspend fun findCodeFragment(
        filePath: String,
        codeFragment: String
    ): String {
        val options = ReplacementOptions(dryRun = true, allowNoChange = true)
        val request = ReplacementRequest(filePath, codeFragment, codeFragment, options)
        val result = codeReplacer.replaceCode(request)
        
        return when (result) {
            is ReplacementResult.DryRun -> {
                if (result.matchesFound > 0) {
                    "Found ${result.matchesFound} occurrence(s) of the code fragment in $filePath"
                } else {
                    "Code fragment not found in $filePath"
                }
            }
            is ReplacementResult.NotFound -> "Code fragment not found in $filePath"
            is ReplacementResult.Error -> "Error searching file: ${result.message}"
            else -> "Unexpected result while searching for code fragment"
        }
    }
    
    /**
     * Replaces multiple code fragments in a single file.
     * 
     * @param filePath Path to the source file
     * @param replacements List of (originalCode, newCode) pairs
     * @param createBackup Whether to create a backup before any changes
     * @return String message with results of all replacements
     */
    suspend fun replaceMultipleFragments(
        filePath: String,
        replacements: List<Pair<String, String>>,
        createBackup: Boolean = true
    ): String {
        if (replacements.isEmpty()) {
            return "No replacements specified"
        }
        
        val results = mutableListOf<String>()
        var successCount = 0
        var errorCount = 0
        
        // Create backup before first replacement if requested
        val options = ReplacementOptions(
            createBackup = createBackup && replacements.isNotEmpty()
        )
        
        for ((index, replacement) in replacements.withIndex()) {
            val (originalCode, newCode) = replacement
            
            // Only create backup on first replacement
            val currentOptions = if (index == 0) options else options.copy(createBackup = false)
            val request = ReplacementRequest(filePath, originalCode, newCode, currentOptions)
            val result = codeReplacer.replaceCode(request)
            
            when {
                result.isSuccess() -> {
                    successCount++
                    results.add("✅ Replaced: ${originalCode.take(50)}${if (originalCode.length > 50) "..." else ""}")
                }
                result.isError() -> {
                    errorCount++
                    results.add("❌ Failed: ${result.getErrorMessage()}")
                }
                else -> {
                    results.add("ℹ️ ${formatResult(result)}")
                }
            }
        }
        
        val summary = "\nSummary: $successCount successful, $errorCount failed out of ${replacements.size} replacements"
        return results.joinToString("\n") + summary
    }
    
    /**
     * Formats a ReplacementResult into a human-readable string.
     */
    private fun formatResult(result: ReplacementResult): String {
        return when (result) {
            is ReplacementResult.Success -> {
                val backupInfo = if (result.backupPath != null) " (backup created: ${result.backupPath})" else ""
                "✅ ${result.message}$backupInfo"
            }
            
            is ReplacementResult.NotFound -> {
                "❌ Code fragment not found in ${result.filePath} (searched ${result.searchedLines} lines)"
            }
            
            is ReplacementResult.MultipleMatches -> {
                val locations = result.matches.joinToString(", ") { 
                    "line ${it.startLine}" 
                }
                "⚠️ Multiple matches found at: $locations. ${result.message}"
            }
            
            is ReplacementResult.Error -> {
                "❌ Error: ${result.message}"
            }
            
            is ReplacementResult.DryRun -> {
                if (result.matchesFound > 0) {
                    val locations = result.matches.joinToString(", ") { match ->
                        match.getLocationDescription()
                    }
                    "📋 Dry run: Found ${result.matchesFound} match(es) at $locations"
                } else {
                    "📋 Dry run: No matches found"
                }
            }
            
            is ReplacementResult.NoChange -> {
                "ℹ️ ${result.message}"
            }
        }
    }
    
    /**
     * Gets detailed information about what would be replaced.
     * 
     * @param filePath Path to the source file
     * @param originalCode Original code fragment to find
     * @param newCode New code that would replace it
     * @return Detailed preview information
     */
    suspend fun getReplacementPreview(
        filePath: String,
        originalCode: String,
        newCode: String
    ): String {
        val options = ReplacementOptions(dryRun = true)
        val request = ReplacementRequest(filePath, originalCode, newCode, options)
        val result = codeReplacer.replaceCode(request)
        
        return when (result) {
            is ReplacementResult.DryRun -> {
                if (result.matchesFound == 0) {
                    "No matches found for the specified code fragment."
                } else {
                    buildString {
                        appendLine("Found ${result.matchesFound} match(es):")
                        appendLine()
                        
                        result.matches.forEachIndexed { index, match ->
                            appendLine("Match ${index + 1} at ${match.getLocationDescription()}:")
                            appendLine("Context before:")
                            appendLine(match.contextBefore)
                            appendLine(">>> MATCHED CODE <<<")
                            appendLine(match.matchedText)
                            appendLine(">>> WOULD BECOME <<<")
                            appendLine(newCode)
                            appendLine("Context after:")
                            appendLine(match.contextAfter)
                            appendLine("-".repeat(50))
                        }
                    }
                }
            }
            
            is ReplacementResult.NotFound -> "Code fragment not found in the file."
            is ReplacementResult.Error -> "Error: ${result.message}"
            else -> "Unexpected result type: ${result::class.simpleName}"
        }
    }
}
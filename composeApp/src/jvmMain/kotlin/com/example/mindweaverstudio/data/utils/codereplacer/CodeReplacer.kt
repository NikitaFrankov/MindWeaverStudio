package com.example.mindweaverstudio.data.utils.codereplacer

import com.example.mindweaverstudio.data.utils.codereplacer.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Core implementation for replacing code fragments in source files.
 */
class CodeReplacer {
    
    /**
     * Replaces code in a file based on the replacement request.
     * 
     * @param request The replacement request containing file path, original code, new code, and options
     * @return ReplacementResult indicating success, error, or other outcomes
     */
    suspend fun replaceCode(request: ReplacementRequest): ReplacementResult = withContext(Dispatchers.IO) {
        try {
            // Validate the request
            when (val validation = request.validate()) {
                is ReplacementValidation.Invalid -> {
                    return@withContext ReplacementResult.Error(validation.message)
                }
                is ReplacementValidation.Valid -> {
                    // Continue with replacement
                }
            }
            
            val filePath = request.getPath()
            
            // Check if file exists and is readable
            if (!validateFile(filePath)) {
                return@withContext ReplacementResult.Error(
                    "File does not exist or is not readable: ${request.filePath}"
                )
            }
            
            // Read file content
            val originalContent = try {
                filePath.readText()
            } catch (e: IOException) {
                return@withContext ReplacementResult.Error(
                    "Failed to read file: ${e.message}",
                    cause = e,
                    filePath = request.filePath
                )
            }
            
            // Process line endings if requested
            val processedOriginalCode = if (request.options.normalizeLineEndings) {
                normalizeLineEndings(request.originalCode)
            } else {
                request.originalCode
            }
            
            val processedContent = if (request.options.normalizeLineEndings) {
                normalizeLineEndings(originalContent)
            } else {
                originalContent
            }
            
            // Find matches
            val matches = findMatches(processedContent, processedOriginalCode, request.options)
            
            when {
                matches.isEmpty() -> {
                    ReplacementResult.NotFound(
                        filePath = request.filePath,
                        originalCode = request.originalCode,
                        searchedLines = processedContent.lines().size
                    )
                }
                
                matches.size > 1 && !request.options.allowMultipleMatches -> {
                    ReplacementResult.MultipleMatches(
                        filePath = request.filePath,
                        matches = matches,
                        message = "Found ${matches.size} matches. Use allowMultipleMatches=true to replace all."
                    )
                }
                
                request.options.dryRun -> {
                    val previewContent = performReplacement(processedContent, matches, request.newCode)
                    ReplacementResult.DryRun(
                        filePath = request.filePath,
                        matchesFound = matches.size,
                        matches = matches,
                        previewContent = previewContent,
                        message = "Dry run: Would replace ${matches.size} occurrence(s)"
                    )
                }
                
                request.originalCode == request.newCode -> {
                    ReplacementResult.NoChange(
                        filePath = request.filePath,
                        message = "No changes made - original and new code are identical"
                    )
                }
                
                else -> {
                    // Perform actual replacement
                    performFileReplacement(request, processedContent, matches)
                }
            }
            
        } catch (e: Exception) {
            ReplacementResult.Error(
                message = "Unexpected error during replacement: ${e.message}",
                cause = e,
                filePath = request.filePath
            )
        }
    }
    
    /**
     * Validates that the file exists and is readable.
     */
    private fun validateFile(filePath: Path): Boolean {
        return filePath.exists() && filePath.isRegularFile() && Files.isReadable(filePath)
    }
    
    /**
     * Normalizes line endings to \n.
     */
    private fun normalizeLineEndings(text: String): String {
        return text.replace("\r\n", "\n").replace("\r", "\n")
    }
    
    /**
     * Finds all matches of the original code in the file content.
     */
    private fun findMatches(
        content: String,
        originalCode: String,
        options: ReplacementOptions
    ): List<ReplacementMatch> {
        val matches = mutableListOf<ReplacementMatch>()
        val lines = content.lines()
        
        if (options.exactMatch) {
            // Find exact string matches
            var searchIndex = 0
            while (true) {
                val matchIndex = content.indexOf(originalCode, searchIndex)
                if (matchIndex == -1) break
                
                val match = createReplacementMatch(content, lines, matchIndex, originalCode.length)
                matches.add(match)
                
                searchIndex = matchIndex + originalCode.length
            }
        } else {
            // For non-exact matching, we could add fuzzy logic here in the future
            // For now, fallback to exact matching
            return findMatches(content, originalCode, options.copy(exactMatch = true))
        }
        
        return matches
    }
    
    /**
     * Creates a ReplacementMatch object for a found match.
     */
    private fun createReplacementMatch(
        content: String,
        lines: List<String>,
        matchIndex: Int,
        matchLength: Int
    ): ReplacementMatch {
        val beforeMatch = content.substring(0, matchIndex)
        val afterMatch = content.substring(matchIndex + matchLength)
        val matchedText = content.substring(matchIndex, matchIndex + matchLength)
        
        // Calculate line and column positions
        val startLine = beforeMatch.count { it == '\n' } + 1
        val lastNewlineBeforeMatch = beforeMatch.lastIndexOf('\n')
        val startColumn = if (lastNewlineBeforeMatch == -1) {
            matchIndex + 1
        } else {
            matchIndex - lastNewlineBeforeMatch
        }
        
        val matchContent = content.substring(matchIndex, matchIndex + matchLength)
        val endLine = startLine + matchContent.count { it == '\n' }
        val endColumn = if (matchContent.contains('\n')) {
            val lastNewlineInMatch = matchContent.lastIndexOf('\n')
            matchContent.length - lastNewlineInMatch
        } else {
            startColumn + matchContent.length - 1
        }
        
        // Get context lines
        val contextBefore = getContextLines(lines, startLine - 1, -2, 0)
        val contextAfter = getContextLines(lines, endLine - 1, 1, 2)
        
        return ReplacementMatch(
            startLine = startLine,
            endLine = endLine,
            startColumn = startColumn,
            endColumn = endColumn,
            matchedText = matchedText,
            contextBefore = contextBefore,
            contextAfter = contextAfter
        )
    }
    
    /**
     * Gets context lines around a match.
     */
    private fun getContextLines(
        lines: List<String>,
        centerLine: Int,
        startOffset: Int,
        endOffset: Int
    ): String {
        val start = maxOf(0, centerLine + startOffset)
        val end = minOf(lines.size - 1, centerLine + endOffset)
        
        return (start..end).mapNotNull { index ->
            lines.getOrNull(index)
        }.joinToString("\n")
    }
    
    /**
     * Performs the actual text replacement.
     */
    private fun performReplacement(
        content: String,
        matches: List<ReplacementMatch>,
        newCode: String
    ): String {
        var result = content
        
        // Replace matches in reverse order to maintain correct indices
        matches.sortedByDescending { match ->
            content.indexOf(match.matchedText)
        }.forEach { match ->
            val matchIndex = result.indexOf(match.matchedText)
            if (matchIndex != -1) {
                result = result.substring(0, matchIndex) + 
                        newCode + 
                        result.substring(matchIndex + match.matchedText.length)
            }
        }
        
        return result
    }
    
    /**
     * Performs the file replacement with backup and safety checks.
     */
    private suspend fun performFileReplacement(
        request: ReplacementRequest,
        content: String,
        matches: List<ReplacementMatch>
    ): ReplacementResult = withContext(Dispatchers.IO) {
        val filePath = request.getPath()
        var backupPath: String? = null
        
        try {
            // Create backup if requested
            if (request.options.createBackup) {
                backupPath = createBackup(filePath)
            }
            
            // Perform replacement
            val newContent = performReplacement(content, matches, request.newCode)
            
            // Write the new content atomically
            writeFileAtomically(filePath, newContent)
            
            ReplacementResult.Success(
                filePath = request.filePath,
                matchesFound = matches.size,
                matchesReplaced = matches.size,
                backupPath = backupPath,
                message = "Successfully replaced ${matches.size} occurrence(s) in ${request.filePath}"
            )
            
        } catch (e: Exception) {
            // If we created a backup and something went wrong, try to restore
            if (backupPath != null) {
                try {
                    Files.copy(
                        Path.of(backupPath),
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (restoreException: Exception) {
                    // Log but don't override the original exception
                }
            }
            
            ReplacementResult.Error(
                message = "Failed to replace code: ${e.message}",
                cause = e,
                filePath = request.filePath
            )
        }
    }
    
    /**
     * Creates a backup of the file.
     */
    private fun createBackup(filePath: Path): String {
        val timestamp = System.currentTimeMillis()
        val backupPath = Path.of("${filePath}.backup.$timestamp")
        Files.copy(filePath, backupPath, StandardCopyOption.COPY_ATTRIBUTES)
        return backupPath.toString()
    }
    
    /**
     * Writes content to file atomically by writing to a temporary file first.
     */
    private fun writeFileAtomically(filePath: Path, content: String) {
        val tempPath = Path.of("${filePath}.tmp.${System.currentTimeMillis()}")
        
        try {
            // Write to temporary file
            tempPath.writeText(content)
            
            // Atomically move temporary file to target
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING)
            
        } catch (e: Exception) {
            // Clean up temporary file if something went wrong
            try {
                Files.deleteIfExists(tempPath)
            } catch (cleanupException: Exception) {
                // Ignore cleanup errors
            }
            throw e
        }
    }
}
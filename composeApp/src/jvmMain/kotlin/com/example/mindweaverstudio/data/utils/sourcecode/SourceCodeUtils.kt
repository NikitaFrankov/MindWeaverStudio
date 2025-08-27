package com.example.mindweaverstudio.data.utils.sourcecode

import com.example.mindweaverstudio.data.utils.sourcecode.models.SearchResult
import com.example.mindweaverstudio.data.utils.sourcecode.models.SourceCodeMatch
import com.example.mindweaverstudio.data.utils.sourcecode.models.getFirstMatch

/**
 * Utility functions for finding and extracting source code from Kotlin projects.
 */
object SourceCodeUtils {
    
    private val finder = SourceCodeFinder()
    private val formatter = SourceCodeFormatter()
    
    /**
     * Finds and returns the formatted source code of a class or function.
     * 
     * @param projectRoot Path to the root of the project
     * @param targetName Name of the class or function to find
     * @param exactMatch If true, searches for exact name match only (default: true)
     * @return Formatted source code with imports and metadata, or error message
     */
    suspend fun findAndFormatSourceCode(
        projectRoot: String,
        targetName: String,
        exactMatch: Boolean = true
    ): String {
        val result = finder.findSourceCode(projectRoot, targetName, exactMatch)
        return formatter.formatResult(result)
    }
    
    /**
     * Finds and returns the clean source code (without metadata comments) of a class or function.
     * 
     * @param projectRoot Path to the root of the project
     * @param targetName Name of the class or function to find
     * @param exactMatch If true, searches for exact name match only (default: true)
     * @return Clean source code with imports, or null if not found/error
     */
    suspend fun findCleanSourceCode(
        projectRoot: String,
        targetName: String,
        exactMatch: Boolean = true
    ): String? {
        val result = finder.findSourceCode(projectRoot, targetName, exactMatch)
        val match = result.getFirstMatch()
        return match?.let { formatter.getCleanSourceCode(it) }
    }
    
    /**
     * Finds source code and returns detailed search result.
     * 
     * @param projectRoot Path to the root of the project
     * @param targetName Name of the class or function to find
     * @param exactMatch If true, searches for exact name match only (default: true)
     * @return SearchResult with all found matches or error information
     */
    suspend fun findSourceCodeDetailed(
        projectRoot: String,
        targetName: String,
        exactMatch: Boolean = true
    ): SearchResult {
        return finder.findSourceCode(projectRoot, targetName, exactMatch)
    }
    
    /**
     * Finds all matches for a target name and returns them as a list.
     * 
     * @param projectRoot Path to the root of the project
     * @param targetName Name of the class or function to find
     * @param exactMatch If true, searches for exact name match only (default: true)
     * @return List of all matches found, empty if none found
     */
    suspend fun findAllMatches(
        projectRoot: String,
        targetName: String,
        exactMatch: Boolean = true
    ): List<SourceCodeMatch> {
        val result = finder.findSourceCode(projectRoot, targetName, exactMatch)
        return when (result) {
            is SearchResult.Success -> result.matches
            is SearchResult.MultipleMatches -> result.matches
            else -> emptyList()
        }
    }
    
    /**
     * Checks if a class or function exists in the project.
     * 
     * @param projectRoot Path to the root of the project
     * @param targetName Name of the class or function to find
     * @param exactMatch If true, searches for exact name match only (default: true)
     * @return True if found, false otherwise
     */
    suspend fun exists(
        projectRoot: String,
        targetName: String,
        exactMatch: Boolean = true
    ): Boolean {
        val result = finder.findSourceCode(projectRoot, targetName, exactMatch)
        return result is SearchResult.Success || result is SearchResult.MultipleMatches
    }
    
    /**
     * Finds a class or function by name with package disambiguation.
     * 
     * @param projectRoot Path to the root of the project
     * @param targetName Name of the class or function to find
     * @param packageName Package name to narrow down the search (optional)
     * @param exactMatch If true, searches for exact name match only (default: true)
     * @return Formatted source code or error message
     */
    suspend fun findInPackage(
        projectRoot: String,
        targetName: String,
        packageName: String? = null,
        exactMatch: Boolean = true
    ): String {
        val result = finder.findSourceCode(projectRoot, targetName, exactMatch)
        
        return when (result) {
            is SearchResult.Success -> {
                if (packageName == null) {
                    formatter.formatResult(result)
                } else {
                    val filteredMatches = result.matches.filter { 
                        it.packageName?.contains(packageName) == true 
                    }
                    if (filteredMatches.isEmpty()) {
                        "Entity '$targetName' not found in package '$packageName'"
                    } else {
                        formatter.formatResult(SearchResult.Success(filteredMatches))
                    }
                }
            }
            is SearchResult.MultipleMatches -> {
                if (packageName == null) {
                    formatter.formatResult(result)
                } else {
                    val filteredMatches = result.matches.filter { 
                        it.packageName?.contains(packageName) == true 
                    }
                    when (filteredMatches.size) {
                        0 -> "Entity '$targetName' not found in package '$packageName'"
                        1 -> formatter.formatResult(SearchResult.Success(filteredMatches))
                        else -> formatter.formatResult(SearchResult.MultipleMatches(filteredMatches))
                    }
                }
            }
            else -> formatter.formatResult(result)
        }
    }
}
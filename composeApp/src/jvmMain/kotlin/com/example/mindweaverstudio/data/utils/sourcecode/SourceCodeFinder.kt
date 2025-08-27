package com.example.mindweaverstudio.data.utils.sourcecode

import com.example.mindweaverstudio.data.utils.sourcecode.models.SearchResult
import com.example.mindweaverstudio.data.utils.sourcecode.models.SourceCodeMatch
import com.example.mindweaverstudio.data.utils.sourcecode.models.EntityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

class SourceCodeFinder {
    
    /**
     * Finds the source code of a class or function by name in a Kotlin project.
     * 
     * @param projectRoot Path to the root of the project
     * @param targetName Name of the class or function to find
     * @param exactMatch If true, searches for exact name match only
     * @return SearchResult containing found matches or error information
     */
    suspend fun findSourceCode(
        projectRoot: String,
        targetName: String,
        exactMatch: Boolean = true
    ): SearchResult = withContext(Dispatchers.IO) {
        try {
            val rootPath = Paths.get(projectRoot)
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                return@withContext SearchResult.Error("Project root directory does not exist: $projectRoot")
            }
            
            val kotlinFiles = findKotlinFiles(rootPath)
            if (kotlinFiles.isEmpty()) {
                return@withContext SearchResult.Error("No Kotlin files found in project: $projectRoot")
            }
            
            val matches = mutableListOf<SourceCodeMatch>()
            var searchedFiles = 0
            
            for (file in kotlinFiles) {
                try {
                    val fileMatches = searchInFile(file, targetName, exactMatch)
                    matches.addAll(fileMatches)
                    searchedFiles++
                } catch (e: Exception) {
                    // Continue searching in other files, but log the error
                    continue
                }
            }
            
            return@withContext when {
                matches.isEmpty() -> SearchResult.NotFound(targetName, searchedFiles)
                matches.size == 1 -> SearchResult.Success(matches)
                else -> SearchResult.MultipleMatches(matches)
            }
            
        } catch (e: Exception) {
            SearchResult.Error("Error during search: ${e.message}", e)
        }
    }
    
    /**
     * Finds all Kotlin files in the project directory recursively.
     */
    private fun findKotlinFiles(rootPath: Path): List<Path> {
        val kotlinFiles = mutableListOf<Path>()
        
        try {
            Files.walk(rootPath)
                .filter { it.isRegularFile() }
                .filter { it.extension == "kt" }
                .forEach { kotlinFiles.add(it) }
        } catch (e: Exception) {
            // Return what we have so far
        }
        
        return kotlinFiles
    }
    
    /**
     * Searches for the target entity in a single Kotlin file.
     */
    private fun searchInFile(filePath: Path, targetName: String, exactMatch: Boolean): List<SourceCodeMatch> {
        val content = Files.readString(filePath)
        val lines = content.lines()
        val matches = mutableListOf<SourceCodeMatch>()
        
        val packageName = extractPackageName(content)
        val imports = extractImports(content)
        
        // Search for different types of entities
        matches.addAll(findClasses(lines, targetName, exactMatch, filePath.toString(), packageName, imports))
        matches.addAll(findFunctions(lines, targetName, exactMatch, filePath.toString(), packageName, imports))
        matches.addAll(findConstants(lines, targetName, exactMatch, filePath.toString(), packageName, imports))
        
        return matches
    }
    
    /**
     * Extracts package name from Kotlin file content.
     */
    private fun extractPackageName(content: String): String? {
        val packageRegex = Regex("""^package\s+([a-zA-Z][a-zA-Z0-9._]*)""", RegexOption.MULTILINE)
        return packageRegex.find(content)?.groupValues?.get(1)
    }
    
    /**
     * Extracts import statements from Kotlin file content.
     */
    private fun extractImports(content: String): List<String> {
        val importRegex = Regex("""^import\s+([a-zA-Z][a-zA-Z0-9._*]*)""", RegexOption.MULTILINE)
        return importRegex.findAll(content).map { it.groupValues[1] }.toList()
    }
    
    /**
     * Finds class, interface, object declarations in the file.
     */
    private fun findClasses(
        lines: List<String>,
        targetName: String,
        exactMatch: Boolean,
        filePath: String,
        packageName: String?,
        imports: List<String>
    ): List<SourceCodeMatch> {
        val matches = mutableListOf<SourceCodeMatch>()
        
        val classPatterns = mapOf(
            EntityType.CLASS to Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?(abstract\s+|open\s+|final\s+)?class\s+(\w+)"""),
            EntityType.INTERFACE to Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?interface\s+(\w+)"""),
            EntityType.OBJECT to Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?object\s+(\w+)"""),
            EntityType.DATA_CLASS to Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?data\s+class\s+(\w+)"""),
            EntityType.SEALED_CLASS to Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?sealed\s+class\s+(\w+)"""),
            EntityType.ENUM_CLASS to Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?enum\s+class\s+(\w+)"""),
            EntityType.ANNOTATION_CLASS to Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?annotation\s+class\s+(\w+)""")
        )
        
        for ((lineIndex, line) in lines.withIndex()) {
            for ((entityType, pattern) in classPatterns) {
                val matchResult = pattern.find(line)
                if (matchResult != null) {
                    val extractedName = matchResult.groupValues.last()
                    if (isNameMatch(extractedName, targetName, exactMatch)) {
                        val sourceCode = extractEntitySourceCode(lines, lineIndex, entityType)
                        matches.add(
                            SourceCodeMatch(
                                entityName = extractedName,
                                entityType = entityType,
                                sourceCode = sourceCode,
                                filePath = filePath,
                                packageName = packageName,
                                lineNumber = lineIndex + 1,
                                imports = imports
                            )
                        )
                    }
                }
            }
        }
        
        return matches
    }
    
    /**
     * Finds function declarations in the file.
     */
    private fun findFunctions(
        lines: List<String>,
        targetName: String,
        exactMatch: Boolean,
        filePath: String,
        packageName: String?,
        imports: List<String>
    ): List<SourceCodeMatch> {
        val matches = mutableListOf<SourceCodeMatch>()
        
        val functionPattern = Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?(suspend\s+|inline\s+|infix\s+|operator\s+)*fun\s+(\w+)""")
        
        for ((lineIndex, line) in lines.withIndex()) {
            val matchResult = functionPattern.find(line)
            if (matchResult != null) {
                val extractedName = matchResult.groupValues.last()
                if (isNameMatch(extractedName, targetName, exactMatch)) {
                    val sourceCode = extractFunctionSourceCode(lines, lineIndex)
                    matches.add(
                        SourceCodeMatch(
                            entityName = extractedName,
                            entityType = EntityType.FUNCTION,
                            sourceCode = sourceCode,
                            filePath = filePath,
                            packageName = packageName,
                            lineNumber = lineIndex + 1,
                            imports = imports
                        )
                    )
                }
            }
        }
        
        return matches
    }
    
    /**
     * Finds constant and property declarations in the file.
     */
    private fun findConstants(
        lines: List<String>,
        targetName: String,
        exactMatch: Boolean,
        filePath: String,
        packageName: String?,
        imports: List<String>
    ): List<SourceCodeMatch> {
        val matches = mutableListOf<SourceCodeMatch>()
        
        val constantPattern = Regex("""^\s*(public\s+|private\s+|internal\s+|protected\s+)?(const\s+)?val\s+(\w+)""")
        
        for ((lineIndex, line) in lines.withIndex()) {
            val matchResult = constantPattern.find(line)
            if (matchResult != null) {
                val extractedName = matchResult.groupValues.last()
                if (isNameMatch(extractedName, targetName, exactMatch)) {
                    val entityType = if (line.contains("const")) EntityType.CONSTANT else EntityType.PROPERTY
                    val sourceCode = extractPropertySourceCode(lines, lineIndex)
                    matches.add(
                        SourceCodeMatch(
                            entityName = extractedName,
                            entityType = entityType,
                            sourceCode = sourceCode,
                            filePath = filePath,
                            packageName = packageName,
                            lineNumber = lineIndex + 1,
                            imports = imports
                        )
                    )
                }
            }
        }
        
        return matches
    }
    
    /**
     * Checks if the extracted name matches the target name.
     */
    private fun isNameMatch(extractedName: String, targetName: String, exactMatch: Boolean): Boolean {
        return if (exactMatch) {
            extractedName == targetName
        } else {
            extractedName.contains(targetName, ignoreCase = true)
        }
    }
    
    /**
     * Extracts the complete source code for a class/interface/object entity.
     */
    private fun extractEntitySourceCode(lines: List<String>, startIndex: Int, entityType: EntityType): String {
        val sourceLines = mutableListOf<String>()
        var braceCount = 0
        var started = false
        
        for (i in startIndex until lines.size) {
            val line = lines[i]
            sourceLines.add(line)
            
            // Count braces to determine the end of the entity
            for (char in line) {
                when (char) {
                    '{' -> {
                        braceCount++
                        started = true
                    }
                    '}' -> {
                        braceCount--
                        if (started && braceCount == 0) {
                            return sourceLines.joinToString("\n")
                        }
                    }
                }
            }
        }
        
        return sourceLines.joinToString("\n")
    }
    
    /**
     * Extracts the complete source code for a function.
     */
    private fun extractFunctionSourceCode(lines: List<String>, startIndex: Int): String {
        val sourceLines = mutableListOf<String>()
        var braceCount = 0
        var started = false
        var hasBody = false
        
        for (i in startIndex until lines.size) {
            val line = lines[i]
            sourceLines.add(line)
            
            // Check if function has a body (contains '{') or is a single expression (contains '=')
            if (!hasBody && (line.contains('{') || line.contains('='))) {
                hasBody = true
            }
            
            // If function is a single expression function, look for the end
            if (line.contains('=') && !line.contains('{')) {
                // Single expression function - continue until we find the complete expression
                if (line.trim().endsWith('}') || line.trim().endsWith(')')) {
                    return sourceLines.joinToString("\n")
                }
                continue
            }
            
            // Count braces for functions with body
            for (char in line) {
                when (char) {
                    '{' -> {
                        braceCount++
                        started = true
                    }
                    '}' -> {
                        braceCount--
                        if (started && braceCount == 0) {
                            return sourceLines.joinToString("\n")
                        }
                    }
                }
            }
            
            // If we haven't found a body after a reasonable number of lines, assume it's abstract
            if (!hasBody && i - startIndex > 3) {
                return sourceLines.joinToString("\n")
            }
        }
        
        return sourceLines.joinToString("\n")
    }
    
    /**
     * Extracts the complete source code for a property/constant.
     */
    private fun extractPropertySourceCode(lines: List<String>, startIndex: Int): String {
        val line = lines[startIndex]
        
        // Simple property/constant declarations are typically single line
        if (!line.contains('{')) {
            return line
        }
        
        // Property with getter/setter - extract the complete block
        return extractEntitySourceCode(lines, startIndex, EntityType.PROPERTY)
    }
}
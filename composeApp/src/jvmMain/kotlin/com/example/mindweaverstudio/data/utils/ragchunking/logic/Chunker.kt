package com.example.mindweaverstudio.data.utils.ragchunking.logic

import com.example.mindweaverstudio.data.utils.ragchunking.models.*
import java.io.File

/**
 * Truly streaming chunker that processes chunks one by one without accumulating them in memory.
 * This is the key to processing very large files without memory issues.
 */
class Chunker(
    private val parser: KotlinCodeParser = KotlinCodeParser(),
    private val maxChunkSize: Int = 300,
    private val largeElementThreshold: Int = 200,
    private val overlapSize: Int = 7,
    private val maxContentLength: Int = 10000
) {
    
    /**
     * Process a file with streaming callback - no chunks are accumulated in memory.
     */
    fun processFileStreaming(file: File, processor: ChunkProcessor): Int {
        if (!file.exists() || !file.isFile) {
            return 0
        }
        
        // Check file size first
        val fileSize = file.length()
        if (fileSize > 1_000_000) { // 1MB limit for individual files
            println("  Skipping very large file: ${file.name} (${fileSize / 1024}KB)")
            return 0
        }
        
        return try {
            // Parse file structure - this gives us metadata without full content
            val analysis = parser.parseFile(file)
            val fileLines = file.readLines() // We need this for content extraction
            
            var chunkCount = 0
            
            // Process each element immediately without accumulating
            for (element in analysis.elements) {
                try {
                    chunkCount += processElementStreaming(element, analysis, fileLines, processor)
                    
                    // Suggest GC after processing large elements
                    if (element.endLine - element.startLine > 100) {
                        System.gc()
                    }
                    
                } catch (e: Exception) {
                    println("    Error processing element ${element.name}: ${e.message}")
                    continue // Skip this element, continue with others
                }
            }
            
            chunkCount
            
        } catch (e: OutOfMemoryError) {
            println("    OutOfMemoryError processing ${file.name} - file too large")
            System.gc()
            0
        } catch (e: Exception) {
            println("    Error processing file ${file.name}: ${e.message}")
            0
        }
    }
    
    /**
     * Process a single code element with streaming - no chunk accumulation.
     */
    private fun processElementStreaming(
        element: CodeElement,
        analysis: FileAnalysis,
        fileLines: List<String>,
        processor: ChunkProcessor
    ): Int {
        val elementSize = element.endLine - element.startLine + 1
        
        // Skip extremely large elements entirely
        if (elementSize > 2000) {
            println("      Skipping extremely large element: ${element.name} ($elementSize lines)")
            return 0
        }
        
        val contextualInfo = buildContextualInfoMinimal(element, analysis)
        
        return if (elementSize > largeElementThreshold) {
            processLargeElementStreaming(element, analysis, fileLines, contextualInfo, processor)
        } else {
            processSingleChunkStreaming(element, analysis, fileLines, contextualInfo, processor)
            1
        }
    }
    
    /**
     * Process a large element by splitting into sub-chunks and streaming each immediately.
     */
    private fun processLargeElementStreaming(
        element: CodeElement,
        analysis: FileAnalysis,
        fileLines: List<String>,
        contextualInfo: ContextualInfo,
        processor: ChunkProcessor
    ): Int {
        val elementLines = try {
            fileLines.subList(element.startLine - 1, minOf(element.endLine, fileLines.size))
        } catch (e: Exception) {
            println("      Error extracting element lines for ${element.name}")
            return 0
        }
        
        val logicalBlocks = findSimpleBlocks(elementLines, element.startLine - 1)
        
        var currentChunkStart = element.startLine
        var chunkCount = 0
        val maxChunksPerElement = 10 // Strict limit
        
        for (block in logicalBlocks) {
            if (chunkCount >= maxChunksPerElement) {
                println("      Reached chunk limit for element: ${element.name}")
                break
            }
            
            val chunkEnd = minOf(block.endLine, element.endLine)
            
            if (chunkEnd <= currentChunkStart || chunkEnd - currentChunkStart < 3) {
                continue
            }
            
            try {
                val chunkContent = buildSubChunkContentMinimal(
                    element, analysis, fileLines, contextualInfo,
                    currentChunkStart, chunkEnd
                )
                
                if (chunkContent.length > maxContentLength) {
                    println("      Chunk content too large, truncating: ${element.name}")
                    continue
                }
                
                val tokens = estimateTokens(chunkContent)
                
                val chunk = ChunkMetadata(
                    filePath = analysis.filePath,
                    className = findContainingClassName(element, analysis),
                    methodName = if (element.type == ChunkType.FUNCTION) element.name else null,
                    startLine = currentChunkStart,
                    endLine = chunkEnd,
                    content = chunkContent,
                    chunkType = ChunkType.SUB_CHUNK,
                    tokens = tokens,
                    contextualInfo = contextualInfo
                )
                
                // Stream chunk immediately
                processor.processChunk(chunk)
                chunkCount++
                
                currentChunkStart = chunkEnd - overlapSize + 1
                
            } catch (e: OutOfMemoryError) {
                println("      OutOfMemoryError creating sub-chunk for ${element.name}")
                System.gc()
                break
            } catch (e: Exception) {
                println("      Error creating sub-chunk: ${e.message}")
                continue
            }
        }
        
        return chunkCount
    }
    
    /**
     * Process a single chunk and stream it immediately.
     */
    private fun processSingleChunkStreaming(
        element: CodeElement,
        analysis: FileAnalysis,
        fileLines: List<String>,
        contextualInfo: ContextualInfo,
        processor: ChunkProcessor
    ) {
        try {
            val chunkContent = buildChunkContentMinimal(element, analysis, fileLines, contextualInfo)
            
            if (chunkContent.length > maxContentLength) {
                println("      Single chunk too large, skipping: ${element.name}")
                return
            }
            
            val tokens = estimateTokens(chunkContent)
            
            val chunk = ChunkMetadata(
                filePath = analysis.filePath,
                className = findContainingClassName(element, analysis),
                methodName = if (element.type == ChunkType.FUNCTION) element.name else null,
                startLine = element.startLine,
                endLine = element.endLine,
                content = chunkContent,
                chunkType = element.type,
                tokens = tokens,
                contextualInfo = contextualInfo
            )
            
            // Stream chunk immediately
            processor.processChunk(chunk)
            
        } catch (e: OutOfMemoryError) {
            println("      OutOfMemoryError creating chunk for ${element.name}")
            System.gc()
        } catch (e: Exception) {
            println("      Error creating chunk: ${e.message}")
        }
    }
    
    /**
     * Find simple logical blocks without complex analysis to save memory.
     */
    private fun findSimpleBlocks(elementLines: List<String>, baseLineIndex: Int): List<LogicalBlock> {
        val blocks = mutableListOf<LogicalBlock>()
        
        // Simple approach: split by fixed size chunks if no logical blocks found
        val chunkSize = maxChunkSize / 2
        for (i in 0 until elementLines.size step chunkSize) {
            val endIndex = minOf(i + chunkSize, elementLines.size)
            blocks.add(LogicalBlock(
                baseLineIndex + i + 1,
                baseLineIndex + endIndex,
                BlockType.SEQUENTIAL
            ))
        }
        
        return blocks.take(10) // Limit number of blocks
    }
    
    /**
     * Build minimal contextual info to save memory.
     */
    private fun buildContextualInfoMinimal(
        element: CodeElement,
        analysis: FileAnalysis
    ): ContextualInfo {
        return ContextualInfo(
            packageDeclaration = analysis.packageDeclaration,
            imports = analysis.imports.take(3), // Only first 3 imports
            classSignature = findContainingClassName(element, analysis)?.let { "$it class signature" },
            relatedPrivateMethods = emptyList() // Skip to save memory
        )
    }
    
    /**
     * Build minimal chunk content to prevent memory issues.
     */
    private fun buildChunkContentMinimal(
        element: CodeElement,
        analysis: FileAnalysis,
        fileLines: List<String>,
        contextualInfo: ContextualInfo
    ): String {
        val content = StringBuilder(2000) // Small initial capacity
        
        // Add minimal context
        contextualInfo.packageDeclaration?.let {
            content.appendLine("package $it")
            content.appendLine()
        }
        
        // Add only essential imports (first 2)
        contextualInfo.imports.take(2).forEach { import ->
            content.appendLine("import $import")
        }
        if (contextualInfo.imports.isNotEmpty()) content.appendLine()
        
        // Add the element content with size limits
        try {
            val elementContent = extractElementContentSafe(element, fileLines)
            if (content.length + elementContent.length > maxContentLength) {
                val availableSpace = maxContentLength - content.length - 50
                if (availableSpace > 50) {
                    content.append(elementContent.take(availableSpace))
                    content.append("\n// ... truncated ...")
                }
            } else {
                content.append(elementContent)
            }
        } catch (e: Exception) {
            content.append("// Error extracting content: ${e.message}")
        }
        
        return content.toString()
    }
    
    /**
     * Build minimal sub-chunk content.
     */
    private fun buildSubChunkContentMinimal(
        element: CodeElement,
        analysis: FileAnalysis,
        fileLines: List<String>,
        contextualInfo: ContextualInfo,
        startLine: Int,
        endLine: Int
    ): String {
        val content = StringBuilder(1500) // Small capacity
        
        // Add minimal context
        contextualInfo.packageDeclaration?.let {
            content.appendLine("package $it")
        }
        
        // Add element signature
        content.appendLine("// ${element.type.name}: ${element.name}")
        content.appendLine(element.signature.take(150)) // Truncated signature
        content.appendLine()
        
        // Add sub-chunk content with bounds checking
        try {
            val actualStartLine = maxOf(0, startLine - 1)
            val actualEndLine = minOf(fileLines.size, endLine)
            
            if (actualStartLine < actualEndLine) {
                val subChunkLines = fileLines.subList(actualStartLine, actualEndLine)
                
                var currentLength = content.length
                for (line in subChunkLines) {
                    if (currentLength + line.length + 1 > maxContentLength) {
                        content.appendLine("// ... truncated ...")
                        break
                    }
                    content.appendLine(line)
                    currentLength = content.length
                }
            }
        } catch (e: Exception) {
            content.appendLine("// Error extracting sub-chunk: ${e.message}")
        }
        
        return content.toString()
    }
    
    /**
     * Safe element content extraction with error handling.
     */
    private fun extractElementContentSafe(element: CodeElement, fileLines: List<String>): String {
        return try {
            if (element.startLine <= 0 || element.endLine > fileLines.size || element.startLine > element.endLine) {
                "// Invalid line numbers: ${element.startLine}-${element.endLine}"
            } else {
                fileLines.subList(element.startLine - 1, element.endLine).joinToString("\n")
            }
        } catch (e: Exception) {
            "// Error extracting content: ${e.message}"
        }
    }
    
    /**
     * Find containing class name without complex analysis.
     */
    private fun findContainingClassName(element: CodeElement, analysis: FileAnalysis): String? {
        return analysis.elements.find { classElement ->
            (classElement.type == ChunkType.CLASS || classElement.type == ChunkType.INTERFACE) &&
            classElement.startLine <= element.startLine &&
            classElement.endLine >= element.endLine &&
            classElement != element
        }?.name
    }
    
    /**
     * Simple token estimation.
     */
    private fun estimateTokens(content: String): Int {
        return minOf(content.length / 4, 1000) // Cap at 1000 tokens
    }
}
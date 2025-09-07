package com.example.mindweaverstudio.data.utils.ragchunking

import com.example.mindweaverstudio.data.utils.ragchunking.logic.Chunker
import com.example.mindweaverstudio.data.utils.ragchunking.logic.RepositoryScanner
import com.example.mindweaverstudio.data.utils.ragchunking.logic.ChunkProcessor
import com.example.mindweaverstudio.data.utils.ragchunking.logic.JsonExporter
import com.example.mindweaverstudio.data.utils.ragchunking.logic.RAGExporter
import com.example.mindweaverstudio.data.utils.ragchunking.logic.StreamingStatistics
import java.io.File

class RAGChunkingUtility(
    maxChunkSize: Int = 300,
    largeElementThreshold: Int = 200,
    overlapSize: Int = 7,
    maxContentLength: Int = 10000
) {
    
    private val chunker = Chunker(
        maxChunkSize = maxChunkSize,
        largeElementThreshold = largeElementThreshold,
        overlapSize = overlapSize,
        maxContentLength = maxContentLength
    )
    
    private val scanner = RepositoryScanner(chunker)
    
    /**
     * Process an entire repository with streaming output to avoid memory issues.
     * Chunks are written to output files immediately as they're generated.
     */
    fun chunkRepositoryStreaming(
        repositoryPath: String,
        outputPath: String,
        includeTests: Boolean = true,
        includeMetadata: Boolean = true
    ): StreamingStatistics {
        println("Starting streaming repository chunking...")
        println("Repository: $repositoryPath")
        println("Output: $outputPath")
        println("Include tests: $includeTests")
        println()
        
        val processor = JsonExporter(outputPath, includeMetadata)
        scanner.scanRepositoryStreaming(repositoryPath, processor, includeTests)
        
        return processor.getStatistics()
    }
    
    /**
     * Process repository with RAG-optimized streaming output.
     * Creates batched JSON files suitable for RAG pipelines.
     */
    fun chunkRepositoryForRAGStreaming(
        repositoryPath: String,
        outputBasePath: String,
        batchSize: Int = 1000,
        includeTests: Boolean = true
    ): StreamingStatistics {
        println("Starting streaming RAG repository chunking...")
        println("Repository: $repositoryPath")
        println("Output base: $outputBasePath")
        println("Batch size: $batchSize")
        println("Include tests: $includeTests")
        println()
        
        val processor = RAGExporter(outputBasePath, batchSize)
        scanner.scanRepositoryStreaming(repositoryPath, processor, includeTests)
        
        return processor.getStatistics()
    }
    
    /**
     * Process a single file with streaming output.
     */
    fun chunkFileStreaming(
        filePath: String,
        outputPath: String,
        includeMetadata: Boolean = true
    ): StreamingStatistics {
        println("Processing single file: $filePath")
        
        val processor = JsonExporter(outputPath, includeMetadata)
        scanner.scanFileStreaming(filePath, processor)
        
        return processor.getStatistics()
    }
    
    /**
     * Process a directory with streaming output.
     */
    fun chunkDirectoryStreaming(
        directoryPath: String,
        outputPath: String,
        includeTests: Boolean = true,
        includeMetadata: Boolean = true
    ): StreamingStatistics {
        println("Processing directory: $directoryPath")
        
        val processor = JsonExporter(outputPath, includeMetadata)
        scanner.scanDirectoryStreaming(directoryPath, processor, includeTests)
        
        return processor.getStatistics()
    }
    
    /**
     * Process with custom streaming processor.
     * Allows for custom handling of chunks (e.g., direct database insertion).
     */
    fun chunkWithCustomProcessor(
        repositoryPath: String,
        processor: ChunkProcessor,
        includeTests: Boolean = true
    ): StreamingStatistics {
        scanner.scanRepositoryStreaming(repositoryPath, processor, includeTests)
        return processor.getStatistics()
    }
    
    /**
     * Complete streaming workflow: chunk repository and create both regular and RAG outputs.
     */
    fun processRepositoryStreaming(
        repositoryPath: String,
        outputBasePath: String,
        includeTests: Boolean = true,
        createRAGOutput: Boolean = true,
        ragBatchSize: Int = 1000
    ) {
        println("=== Streaming RAG Chunking Utility ===")
        println("Processing repository: $repositoryPath")
        println("Memory-efficient streaming mode")
        println()
        
        // Create output directory
        val outputDir = File(outputBasePath).parentFile
        outputDir?.mkdirs()
        
        // Process with regular JSON output
        val regularOutputPath = "${outputBasePath}_chunks.json"
        println("1. Creating regular JSON output...")
        val regularStats = chunkRepositoryStreaming(repositoryPath, regularOutputPath, includeTests)
        
        if (createRAGOutput) {
            println("\n2. Creating RAG-optimized output...")
            val ragOutputPath = "${outputBasePath}_rag.json"
            val ragStats = chunkRepositoryForRAGStreaming(
                repositoryPath, ragOutputPath, ragBatchSize, includeTests
            )
            
            println("\n=== Processing Complete ===")
            println("Files created:")
            println("- Regular chunks: $regularOutputPath")
            println("- RAG batches: ${ragOutputPath.replace(".json", "_batch_*.json")}")
            
            printStatistics(regularStats, "Regular Export")
            printStatistics(ragStats, "RAG Export")
        } else {
            println("\n=== Processing Complete ===")
            println("Files created:")
            println("- Regular chunks: $regularOutputPath")
            
            printStatistics(regularStats, "Export")
        }
    }
    
    private fun printStatistics(stats: StreamingStatistics, label: String) {
        println("\n=== $label Statistics ===")
        println("Files processed: ${stats.totalFiles}")
        println("Chunks generated: ${stats.totalChunks}")
        println("Total tokens: ${stats.totalTokens}")
        println("Average tokens per chunk: ${stats.averageTokensPerChunk}")
        println("Largest chunk: ${stats.largestChunkTokens} tokens")
        println("Smallest chunk: ${if (stats.smallestChunkTokens == Int.MAX_VALUE) 0 else stats.smallestChunkTokens} tokens")
        println("Chunks with overlap: ${stats.chunksWithOverlap}")
        println("Files skipped: ${stats.skippedFiles}")
        println("Files with errors: ${stats.errorFiles}")
        println()
        println("Chunks by type:")
        stats.chunksByType.entries.sortedByDescending { it.value }.forEach { (type, count) ->
            println("  $type: $count")
        }
        println()
    }
    
    companion object Companion {
        /**
         * Quick utility method to chunk the current project with streaming.
         */
        fun chunkCurrentProjectStreaming(
            outputDir: String = "./rag_chunks",
            includeTests: Boolean = false,
            ragBatchSize: Int = 1000
        ) {
            val utility = RAGChunkingUtility()
            val currentDir = System.getProperty("user.dir")
            
            File(outputDir).mkdirs()
            
            utility.processRepositoryStreaming(
                repositoryPath = currentDir,
                outputBasePath = "$outputDir/mindweaver_studio",
                includeTests = includeTests,
                createRAGOutput = true,
                ragBatchSize = ragBatchSize
            )
        }
        
        /**
         * Create a streaming utility with custom configuration.
         */
        fun create(
            maxChunkSize: Int = 300,
            largeElementThreshold: Int = 200,
            overlapSize: Int = 7,
            maxContentLength: Int = 10000
        ): RAGChunkingUtility {
            return RAGChunkingUtility(
                maxChunkSize, largeElementThreshold, overlapSize, maxContentLength
            )
        }
        
        /**
         * Create a memory-optimized streaming utility for large repositories.
         */
        fun createMemoryOptimized(): RAGChunkingUtility {
            return RAGChunkingUtility(
                maxChunkSize = 150,           // Smaller chunks
                largeElementThreshold = 80,   // Split earlier
                overlapSize = 3,              // Minimal overlap
                maxContentLength = 3000       // 3KB limit per chunk
            )
        }
    }
}
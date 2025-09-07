package com.example.mindweaverstudio.data.utils.ragchunking.logic

import com.example.mindweaverstudio.data.utils.ragchunking.models.ChunkMetadata
import com.example.mindweaverstudio.data.utils.ragchunking.models.RAGDocument
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Streaming JSON exporter that writes chunks immediately to avoid memory accumulation.
 */
class JsonExporter(
    private val outputPath: String,
    private val includeMetadata: Boolean = false
) : ChunkProcessor {
    
    private val json = Json {
        prettyPrint = false // Use compact format for streaming
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val writer: BufferedWriter
    private val statistics = StreamingStatistics()
    private var isFirstChunk = true
    
    init {
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        writer = BufferedWriter(FileWriter(file))
        
        // Write JSON opening
        writer.write("{")
        
        if (includeMetadata) {
            writer.write("\"metadata\":{")
            writer.write("\"generatedAt\":\"${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}\",")
            writer.write("\"isStreaming\":true")
            writer.write("},")
        }
        
        writer.write("\"chunks\":[")
        writer.flush()
    }
    
    override fun processChunk(chunk: ChunkMetadata) {
        try {
            // Update statistics
            updateStatistics(chunk)
            
            // Write chunk separator
            if (!isFirstChunk) {
                writer.write(",")
            } else {
                isFirstChunk = false
            }
            
            // Write chunk as JSON
            val chunkJson = json.encodeToString(chunk)
            writer.write(chunkJson)
            writer.flush()
            
            // Suggest GC every 100 chunks to help with memory
            if (statistics.totalChunks % 100 == 0) {
                System.gc()
                
                // Print progress
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val maxMemory = runtime.maxMemory()
                val memoryPercent = (usedMemory.toDouble() / maxMemory * 100).toInt()
                println("  Written ${statistics.totalChunks} chunks, Memory: ${memoryPercent}%")
            }
            
        } catch (e: Exception) {
            println("Error writing chunk: ${e.message}")
            statistics.errorFiles++
        }
    }
    
    override fun onFileComplete(filePath: String, chunkCount: Int) {
        statistics.totalFiles++
        if (chunkCount == 0) {
            statistics.skippedFiles++
        }
    }
    
    override fun onComplete() {
        try {
            // Close JSON array and object
            writer.write("]")
            
            if (includeMetadata) {
                // Add final statistics to metadata
//                writer.write(",\"finalStatistics\":{")
//                writer.write("\"totalFiles\":${statistics.totalFiles},")
//                writer.write("\"totalChunks\":${statistics.totalChunks},")
//                writer.write("\"totalTokens\":${statistics.totalTokens},")
//                writer.write("\"averageTokensPerChunk\":${statistics.averageTokensPerChunk},")
//                writer.write("\"chunksByType\":${json.encodeToString(statistics.chunksByType)},")
//                writer.write("\"largestChunkTokens\":${statistics.largestChunkTokens},")
//                writer.write("\"smallestChunkTokens\":${if (statistics.smallestChunkTokens == Int.MAX_VALUE) 0 else statistics.smallestChunkTokens},")
//                writer.write("\"chunksWithOverlap\":${statistics.chunksWithOverlap},")
//                writer.write("\"skippedFiles\":${statistics.skippedFiles},")
//                writer.write("\"errorFiles\":${statistics.errorFiles}")
//                writer.write("}")
            }
            
            writer.write("}")
            writer.flush()
            writer.close()
            
            println("\nStreaming export completed:")
            println("- Output file: $outputPath")
            println("- Total chunks written: ${statistics.totalChunks}")
            println("- Total files processed: ${statistics.totalFiles}")
            
        } catch (e: Exception) {
            println("Error completing export: ${e.message}")
        }
    }
    
    override fun getStatistics(): StreamingStatistics = statistics
    
    private fun updateStatistics(chunk: ChunkMetadata) {
        statistics.totalChunks++
        statistics.totalTokens += chunk.tokens
        
        val chunkTypeKey = chunk.chunkType.name
        statistics.chunksByType[chunkTypeKey] = statistics.chunksByType.getOrDefault(chunkTypeKey, 0) + 1
        
        if (chunk.tokens > statistics.largestChunkTokens) {
            statistics.largestChunkTokens = chunk.tokens
        }
        
        if (chunk.tokens < statistics.smallestChunkTokens) {
            statistics.smallestChunkTokens = chunk.tokens
        }
        
        if (chunk.overlapsWithPrevious) {
            statistics.chunksWithOverlap++
        }
    }
}

/**
 * Streaming RAG exporter that creates batched RAG-optimized output files.
 */
class RAGExporter(
    private val outputBasePath: String,
    private val batchSize: Int = 1000
) : ChunkProcessor {
    
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val statistics = StreamingStatistics()
    private var currentBatch = mutableListOf<RAGDocument>()
    private var currentBatchId = 1
    private var totalBatches = 0
    
    override fun processChunk(chunk: ChunkMetadata) {
        try {
            updateStatistics(chunk)
            
            // Convert chunk to RAG document
            val ragDoc = RAGDocument(
                id = generateDocumentId(chunk),
                content = chunk.content,
                metadata = mapOf(
                    "filePath" to chunk.filePath,
                    "className" to (chunk.className ?: ""),
                    "methodName" to (chunk.methodName ?: ""),
                    "chunkType" to chunk.chunkType.name,
//                    "startLine" to chunk.startLine.toString(),
//                    "endLine" to chunk.endLine.toString(),
//                    "tokens" to chunk.tokens.toString(),
//                    "overlapsWithPrevious" to chunk.overlapsWithPrevious.toString()
                )
            )
            
            currentBatch.add(ragDoc)
            
            // Write batch if full
            if (currentBatch.size >= batchSize) {
                writeBatch()
            }
            
        } catch (e: Exception) {
            println("Error processing chunk for RAG export: ${e.message}")
            statistics.errorFiles++
        }
    }
    
    override fun onFileComplete(filePath: String, chunkCount: Int) {
        statistics.totalFiles++
        if (chunkCount == 0) {
            statistics.skippedFiles++
        }
    }
    
    override fun onComplete() {
        // Write remaining chunks in final batch
        if (currentBatch.isNotEmpty()) {
            writeBatch()
        }
        
        println("\nRAG streaming export completed:")
        println("- Total batches created: $totalBatches")
        println("- Total chunks exported: ${statistics.totalChunks}")
        println("- Batch files pattern: ${outputBasePath.replace(".json", "_batch_*.json")}")
    }
    
    override fun getStatistics(): StreamingStatistics = statistics
    
    private fun writeBatch() {
        if (currentBatch.isEmpty()) return
        
        try {
            val batchFile = File(outputBasePath.replace(".json", "_batch_${currentBatchId}.json"))
            batchFile.parentFile?.mkdirs()
            
            val ragExport = mapOf(
                "batchId" to currentBatchId,
                "totalBatches" to "TBD", // Will be unknown until the end
                "documents" to currentBatch.toList() // Create a copy
            )
            
            val jsonContent = json.encodeToString(ragExport)
            batchFile.writeText(jsonContent)
            
            println("  Written batch $currentBatchId (${currentBatch.size} documents)")
            
            // Clear current batch and increment
            currentBatch.clear()
            currentBatchId++
            totalBatches++
            
            // Suggest GC after each batch
            System.gc()
            
        } catch (e: Exception) {
            println("Error writing RAG batch: ${e.message}")
            statistics.errorFiles++
        }
    }
    
    private fun generateDocumentId(chunk: ChunkMetadata): String {
        val fileName = File(chunk.filePath).nameWithoutExtension
        val identifier = when {
            chunk.methodName != null -> "${chunk.className}_${chunk.methodName}"
            chunk.className != null -> chunk.className
            else -> fileName
        }
        return "${fileName}_${identifier}_${chunk.startLine}-${chunk.endLine}".replace(" ", "_")
    }
    
    private fun updateStatistics(chunk: ChunkMetadata) {
        statistics.totalChunks++
        statistics.totalTokens += chunk.tokens
        
        val chunkTypeKey = chunk.chunkType.name
        statistics.chunksByType[chunkTypeKey] = statistics.chunksByType.getOrDefault(chunkTypeKey, 0) + 1
        
        if (chunk.tokens > statistics.largestChunkTokens) {
            statistics.largestChunkTokens = chunk.tokens
        }
        
        if (chunk.tokens < statistics.smallestChunkTokens) {
            statistics.smallestChunkTokens = chunk.tokens
        }
        
        if (chunk.overlapsWithPrevious) {
            statistics.chunksWithOverlap++
        }
    }
}
package com.example.mindweaverstudio.data.utils.ragchunking.logic

import com.example.mindweaverstudio.data.utils.ragchunking.RAGChunkingUtility
import com.example.mindweaverstudio.data.utils.ragchunking.models.ChunkMetadata
import com.example.mindweaverstudio.data.utils.ragchunking.models.ChunkType
import java.io.File

/**
 * Example custom processors for different use cases.
 */

/**
 * Statistics-only processor that doesn't write chunks to disk but collects detailed analytics.
 */
class AnalyticsOnlyProcessor : ChunkProcessor {
    private val statistics = StreamingStatistics()
    private val chunkSizes = mutableListOf<Int>()
    private val fileChunkCounts = mutableMapOf<String, Int>()
    
    override fun processChunk(chunk: ChunkMetadata) {
        updateStatistics(chunk)
        chunkSizes.add(chunk.tokens)
        
        val fileName = File(chunk.filePath).nameWithoutExtension
        fileChunkCounts[fileName] = fileChunkCounts.getOrDefault(fileName, 0) + 1
    }
    
    override fun onFileComplete(filePath: String, chunkCount: Int) {
        statistics.totalFiles++
        if (chunkCount == 0) {
            statistics.skippedFiles++
        }
    }
    
    override fun onComplete() {
        println("\n=== Analytics Report ===")
        println("Files analyzed: ${statistics.totalFiles}")
        println("Chunks analyzed: ${statistics.totalChunks}")
        println("Total tokens: ${statistics.totalTokens}")
        
        // Token distribution analysis
        val sortedSizes = chunkSizes.sorted()
        val median = if (sortedSizes.isNotEmpty()) sortedSizes[sortedSizes.size / 2] else 0
        val percentile95 = if (sortedSizes.isNotEmpty()) sortedSizes[(sortedSizes.size * 0.95).toInt()] else 0
        
        println("\nToken Distribution:")
        println("  Minimum: ${statistics.smallestChunkTokens}")
        println("  Median: $median")
        println("  95th percentile: $percentile95")
        println("  Maximum: ${statistics.largestChunkTokens}")
        println("  Average: ${statistics.averageTokensPerChunk}")
        
        // Top files by chunk count
        println("\nTop Files by Chunk Count:")
        fileChunkCounts.entries.sortedByDescending { it.value }.take(10).forEach { (file, count) ->
            println("  $file: $count chunks")
        }
        
        // Chunk type distribution
        println("\nChunk Type Distribution:")
        statistics.chunksByType.entries.sortedByDescending { it.value }.forEach { (type, count) ->
            val percentage = (count.toDouble() / statistics.totalChunks * 100).toInt()
            println("  $type: $count ($percentage%)")
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
 * Filtering processor that only processes chunks matching certain criteria.
 */
class FilteringProcessor(
    private val delegate: ChunkProcessor,
    private val chunkTypeFilter: Set<ChunkType>? = null,
    private val minTokens: Int = 0,
    private val maxTokens: Int = Int.MAX_VALUE,
    private val filePathFilter: ((String) -> Boolean)? = null
) : ChunkProcessor {
    
    private var filteredCount = 0
    
    override fun processChunk(chunk: ChunkMetadata) {
        // Apply filters
        if (chunkTypeFilter != null && chunk.chunkType !in chunkTypeFilter) {
            filteredCount++
            return
        }
        
        if (chunk.tokens < minTokens || chunk.tokens > maxTokens) {
            filteredCount++
            return
        }
        
        if (filePathFilter != null && !filePathFilter.invoke(chunk.filePath)) {
            filteredCount++
            return
        }
        
        // Chunk passes all filters, delegate to actual processor
        delegate.processChunk(chunk)
    }
    
    override fun onFileComplete(filePath: String, chunkCount: Int) {
        delegate.onFileComplete(filePath, chunkCount)
    }
    
    override fun onComplete() {
        println("Filtered out $filteredCount chunks based on criteria")
        delegate.onComplete()
    }
    
    override fun getStatistics(): StreamingStatistics = delegate.getStatistics()
}

/**
 * Multi-output processor that sends chunks to multiple processors simultaneously.
 */
class MultiOutputProcessor(
    private val processors: List<ChunkProcessor>
) : ChunkProcessor {
    
    override fun processChunk(chunk: ChunkMetadata) {
        processors.forEach { processor ->
            try {
                processor.processChunk(chunk)
            } catch (e: Exception) {
                println("Error in processor ${processor::class.simpleName}: ${e.message}")
            }
        }
    }
    
    override fun onFileComplete(filePath: String, chunkCount: Int) {
        processors.forEach { it.onFileComplete(filePath, chunkCount) }
    }
    
    override fun onComplete() {
        processors.forEach { it.onComplete() }
    }
    
    override fun getStatistics(): StreamingStatistics {
        // Return statistics from the first processor
        return processors.firstOrNull()?.getStatistics() ?: StreamingStatistics()
    }
}

/**
 * Console logging processor that prints chunk information in real-time.
 */
class ConsoleLoggingProcessor(
    private val verbose: Boolean = false
) : ChunkProcessor {
    
    private val statistics = StreamingStatistics()
    
    override fun processChunk(chunk: ChunkMetadata) {
        updateStatistics(chunk)
        
        if (verbose) {
            println("Chunk #${statistics.totalChunks}: ${chunk.chunkType} '${chunk.className ?: chunk.methodName ?: "N/A"}' (${chunk.tokens} tokens)")
        } else if (statistics.totalChunks % 100 == 0) {
            println("Processed ${statistics.totalChunks} chunks...")
        }
    }
    
    override fun onFileComplete(filePath: String, chunkCount: Int) {
        statistics.totalFiles++
        if (chunkCount == 0) {
            statistics.skippedFiles++
        }
        
        if (verbose) {
            val fileName = File(filePath).name
            println("File complete: $fileName -> $chunkCount chunks")
        }
    }
    
    override fun onComplete() {
        println("\n=== Console Logging Summary ===")
        println("Total files: ${statistics.totalFiles}")
        println("Total chunks: ${statistics.totalChunks}")
        println("Total tokens: ${statistics.totalTokens}")
        println("Average tokens per chunk: ${statistics.averageTokensPerChunk}")
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
 * Example usage of custom processors
 */
object CustomProcessorExamples {
    
    fun runAnalyticsOnly() {
        val utility = RAGChunkingUtility.createMemoryOptimized()
        val processor = AnalyticsOnlyProcessor()
        
        utility.chunkWithCustomProcessor(
            repositoryPath = ".",
            processor = processor,
            includeTests = false
        )
    }
    
    fun runFilteredExport() {
        val utility = RAGChunkingUtility.createMemoryOptimized()
        
        // Only export CLASS and INTERFACE chunks with >100 tokens
        val jsonExporter = JsonExporter("./filtered_chunks.json")
        val filteredProcessor = FilteringProcessor(
            delegate = jsonExporter,
            chunkTypeFilter = setOf(ChunkType.CLASS, ChunkType.INTERFACE),
            minTokens = 100
        )
        
        utility.chunkWithCustomProcessor(
            repositoryPath = ".",
            processor = filteredProcessor,
            includeTests = false
        )
    }
    
    fun runMultiOutput() {
        val utility = RAGChunkingUtility.createMemoryOptimized()
        
        // Export to both regular JSON and RAG format simultaneously
        val processors = listOf(
            JsonExporter("./chunks.json"),
            RAGExporter("./rag_chunks.json", batchSize = 500),
            AnalyticsOnlyProcessor(),
            ConsoleLoggingProcessor(verbose = false)
        )
        
        val multiProcessor = MultiOutputProcessor(processors)
        
        utility.chunkWithCustomProcessor(
            repositoryPath = ".",
            processor = multiProcessor,
            includeTests = false
        )
    }
}
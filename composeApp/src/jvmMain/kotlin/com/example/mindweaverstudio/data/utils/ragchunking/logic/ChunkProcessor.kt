package com.example.mindweaverstudio.data.utils.ragchunking.logic

import com.example.mindweaverstudio.data.utils.ragchunking.models.ChunkMetadata

/**
 * Interface for processing chunks in a streaming manner without accumulating them in memory.
 */
interface ChunkProcessor {
    /**
     * Called when a new chunk is generated.
     * The processor should immediately handle the chunk (write to file, etc.)
     * and not keep it in memory.
     */
    fun processChunk(chunk: ChunkMetadata)
    
    /**
     * Called when processing of a file is complete.
     */
    fun onFileComplete(filePath: String, chunkCount: Int)
    
    /**
     * Called when all processing is complete.
     */
    fun onComplete()
    
    /**
     * Get lightweight statistics without keeping chunk objects in memory.
     */
    fun getStatistics(): StreamingStatistics
}

/**
 * Lightweight statistics that don't require keeping chunk objects in memory.
 */
data class StreamingStatistics(
    var totalFiles: Int = 0,
    var totalChunks: Int = 0,
    var totalTokens: Long = 0,
    var chunksByType: MutableMap<String, Int> = mutableMapOf(),
    var largestChunkTokens: Int = 0,
    var smallestChunkTokens: Int = Int.MAX_VALUE,
    var chunksWithOverlap: Int = 0,
    var skippedFiles: Int = 0,
    var errorFiles: Int = 0
) {
    val averageTokensPerChunk: Int
        get() = if (totalChunks > 0) (totalTokens / totalChunks).toInt() else 0
}
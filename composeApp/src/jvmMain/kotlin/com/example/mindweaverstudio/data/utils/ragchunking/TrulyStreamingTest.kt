package com.example.mindweaverstudio.data.utils.ragchunking

fun main() {
    val utility = RAGChunkingUtility(
        maxChunkSize = 200,          // Very small chunks
        largeElementThreshold = 50,  // Split elements very early
        overlapSize = 2,             // Minimal overlap
        maxContentLength = 2000      // 2KB max per chunk
    )
    
    val repositoryPath = "/Users/nikitaradionov/AndroidStudioProjects/Final_qualifying_work/app/src/main/java/com/gervant08/finalqualifyingwork"
    val outputBasePath = "/Users/nikitaradionov/IdeaProjects/MindWeaver Studio/truly_streaming_output"
    
    try {
        utility.processRepositoryStreaming(
            repositoryPath = repositoryPath,
            outputBasePath = outputBasePath,
            includeTests = false,
            createRAGOutput = true,
            ragBatchSize = 500
        )
        
        println()
        
    } catch (e: OutOfMemoryError) {
        println("❌ OutOfMemoryError still occurred!")
        println("Even the truly streaming approach couldn't handle this repository.")
        println("Recommendations:")
        println("1. Increase JVM heap size: -Xmx8g")
        println("2. Process smaller directories at a time")
        println("3. Use even more aggressive size limits")
        
    } catch (e: Exception) {
        println("❌ Error occurred: ${e.message}")
        e.printStackTrace()
    }
    
    println("\n=== Test Complete ===")
}
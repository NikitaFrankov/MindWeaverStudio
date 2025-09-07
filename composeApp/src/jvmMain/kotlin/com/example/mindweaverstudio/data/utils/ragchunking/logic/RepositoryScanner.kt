package com.example.mindweaverstudio.data.utils.ragchunking.logic

import java.io.File

/**
 * Repository scanner that processes chunks in truly streaming mode to avoid ANY memory accumulation.
 * Uses TrulyStreamingChunker that processes chunks one by one without creating intermediate lists.
 */
class RepositoryScanner(
    private val chunker: Chunker = Chunker()
) {

    private val supportedExtensions = setOf(".kt", ".java")
    private val excludedDirectories = setOf(
        "build", "target", ".gradle", ".idea", ".git", "node_modules",
        ".vscode", "out", "bin", "generated", "resources"
    )

    /**
     * Scan repository and stream chunks to processor immediately.
     * This prevents memory accumulation by not keeping chunks in memory.
     */
    fun scanRepositoryStreaming(
        rootPath: String,
        processor: ChunkProcessor,
        includeTests: Boolean = true
    ) {
        val rootFile = File(rootPath)
        if (!rootFile.exists() || !rootFile.isDirectory) {
            throw IllegalArgumentException("Invalid repository path: $rootPath")
        }

        val sourceFiles = findSourceFiles(rootFile, includeTests)

        println("Found ${sourceFiles.size} source files to process...")
        println("Using streaming mode to avoid memory issues...")

        // Process files one by one, immediately streaming chunks to processor
        sourceFiles.forEachIndexed { index, file ->
            try {
                // Skip very large files to prevent memory issues
                val fileSize = file.length()
                if (fileSize > 500_000) { // 500KB limit
                    println("Skipping large file: ${file.name} (${fileSize / 1024}KB)")
                    processor.onFileComplete(file.absolutePath, 0)
                    return@forEachIndexed
                }

                print("Processing file ${index + 1}/${sourceFiles.size}: ${file.name}...")

                // Process file with true streaming - no chunk lists created at all!
                val chunkCount = chunker.processFileStreaming(file, processor)

                println(" ✓ ${chunkCount} chunks")
                processor.onFileComplete(file.absolutePath, chunkCount)

                // No objects to clean up - everything was streamed directly!

                // Suggest GC every 20 files to help with memory pressure
                if ((index + 1) % 20 == 0) {
                    System.gc()

                    // Print memory status
                    val runtime = Runtime.getRuntime()
                    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                    val maxMemory = runtime.maxMemory()
                    val memoryPercent = (usedMemory.toDouble() / maxMemory * 100).toInt()
                    println("  Memory usage: ${memoryPercent}%")
                }

            } catch (e: Exception) {
                println("Error processing file ${file.absolutePath}: ${e.message}")
                processor.onFileComplete(file.absolutePath, 0)
            }
        }

        // Notify processor that all processing is complete
        processor.onComplete()

        val stats = processor.getStatistics()
        println("\nStreaming scan complete:")
        println("- Files processed: ${stats.totalFiles}")
        println("- Chunks generated: ${stats.totalChunks}")
        println("- Average chunks per file: ${stats.totalChunks / stats.totalFiles.coerceAtLeast(1)}")
        println("- Files skipped: ${stats.skippedFiles}")
        println("- Files with errors: ${stats.errorFiles}")
    }

    /**
     * Scan single file in streaming mode.
     */
    fun scanFileStreaming(
        filePath: String,
        processor: ChunkProcessor
    ) {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("Invalid file path: $filePath")
        }

        if (!isSupportedFile(file)) {
            throw IllegalArgumentException("Unsupported file type: ${file.extension}")
        }

        println("Processing single file: ${file.name}")

        try {
            val chunkCount = chunker.processFileStreaming(file, processor)
            processor.onFileComplete(file.absolutePath, chunkCount)
            println("Generated ${chunkCount} chunks")

        } catch (e: Exception) {
            println("Error processing file: ${e.message}")
            processor.onFileComplete(file.absolutePath, 0)
        }

        processor.onComplete()
    }

    /**
     * Scan directory in streaming mode.
     */
    fun scanDirectoryStreaming(
        directoryPath: String,
        processor: ChunkProcessor,
        includeTests: Boolean = true
    ) {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            throw IllegalArgumentException("Invalid directory path: $directoryPath")
        }

        val sourceFiles = findSourceFiles(directory, includeTests)

        println("Processing directory: $directoryPath")
        println("Found ${sourceFiles.size} source files")

        sourceFiles.forEachIndexed { index, file ->
            try {
                print("Processing ${index + 1}/${sourceFiles.size}: ${file.name}...")

                val chunkCount = chunker.processFileStreaming(file, processor)
                processor.onFileComplete(file.absolutePath, chunkCount)
                println(" ✓ ${chunkCount} chunks")

            } catch (e: Exception) {
                println("Error processing file ${file.absolutePath}: ${e.message}")
                processor.onFileComplete(file.absolutePath, 0)
            }
        }

        processor.onComplete()
    }

    private fun findSourceFiles(rootDirectory: File, includeTests: Boolean): List<File> {
        val sourceFiles = mutableListOf<File>()

        rootDirectory.walkTopDown()
            .filter { file ->
                // Skip excluded directories
                !shouldExcludeDirectory(file, rootDirectory) &&
                // Include only supported files
                file.isFile && isSupportedFile(file) &&
                // Filter test files if needed
                (includeTests || !isTestFile(file))
            }
            .forEach { file ->
                sourceFiles.add(file)
            }

        return sourceFiles
    }

    private fun shouldExcludeDirectory(file: File, rootDirectory: File): Boolean {
        val relativePath = file.relativeTo(rootDirectory).path
        return excludedDirectories.any { excludedDir ->
            relativePath.startsWith(excludedDir) || relativePath.contains("/$excludedDir/")
        }
    }

    private fun isSupportedFile(file: File): Boolean {
        return supportedExtensions.any { extension ->
            file.name.endsWith(extension)
        }
    }

    private fun isTestFile(file: File): Boolean {
        val fileName = file.name.lowercase()
        val pathComponents = file.path.lowercase().split(File.separator)

        return fileName.contains("test") ||
                fileName.contains("spec") ||
                pathComponents.any { it == "test" || it == "tests" || it.contains("test") }
    }
}
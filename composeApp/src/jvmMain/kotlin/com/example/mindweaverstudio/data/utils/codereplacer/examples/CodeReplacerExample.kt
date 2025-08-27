package com.example.mindweaverstudio.data.utils.codereplacer.examples

import com.example.mindweaverstudio.data.utils.codereplacer.CodeReplacerUtils
import com.example.mindweaverstudio.data.utils.codereplacer.models.ReplacementOptions
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Example usage and testing of the CodeReplacer utility.
 * 
 * This example demonstrates how to use the CodeReplacer to safely replace
 * code fragments in source files.
 */
fun main() = runBlocking {
    println("=== CodeReplacer Example ===")
    println()
    
    // Create a test file for demonstration
    val testFile = createTestFile()
    println("Created test file: $testFile")
    println()
    
    try {
        // Example 1: Basic code replacement
        println("1. Basic Code Replacement:")
        println("=" .repeat(40))
        val result1 = CodeReplacerUtils.replaceCodeInFile(
            filePath = testFile,
            originalCode = "fun oldMethod() {\n        println(\"Old implementation\")\n    }",
            newCode = "fun newMethod() {\n        println(\"New and improved implementation\")\n    }"
        )
        println(result1)
        println()
        
        // Example 2: Preview replacement before applying
        println("2. Preview Replacement:")
        println("=" .repeat(40))
        val preview = CodeReplacerUtils.previewReplacement(
            filePath = testFile,
            originalCode = "private val name = \"Example\"",
            newCode = "private val name = \"Updated Example\""
        )
        println(preview)
        println()
        
        // Example 3: Get detailed preview information
        println("3. Detailed Preview:")
        println("=" .repeat(40))
        val detailedPreview = CodeReplacerUtils.getReplacementPreview(
            filePath = testFile,
            originalCode = "private val name = \"Example\"",
            newCode = "private val name = \"Updated Example\""
        )
        println(detailedPreview)
        println()
        
        // Example 4: Replace with backup
        println("4. Replace with Backup:")
        println("=" .repeat(40))
        val result4 = CodeReplacerUtils.replaceCodeWithOptions(
            filePath = testFile,
            originalCode = "private val name = \"Example\"",
            newCode = "private val name = \"Backed Up Example\"",
            createBackup = true,
            allowMultiple = false
        )
        println(result4)
        println()
        
        // Example 5: Find code fragment without replacing
        println("5. Find Code Fragment:")
        println("=" .repeat(40))
        val findResult = CodeReplacerUtils.findCodeFragment(
            filePath = testFile,
            codeFragment = "class ExampleClass"
        )
        println(findResult)
        println()
        
        // Example 6: Multiple replacements in one operation
        println("6. Multiple Replacements:")
        println("=" .repeat(40))
        val replacements = listOf(
            "println(\"New and improved implementation\")" to "println(\"Latest implementation\")",
            "Backed Up Example" to "Final Example"
        )
        val multipleResult = CodeReplacerUtils.replaceMultipleFragments(
            filePath = testFile,
            replacements = replacements,
            createBackup = false
        )
        println(multipleResult)
        println()
        
        // Example 7: Handle non-existent code
        println("7. Handle Non-Existent Code:")
        println("=" .repeat(40))
        val notFoundResult = CodeReplacerUtils.replaceCodeInFile(
            filePath = testFile,
            originalCode = "nonExistentFunction()",
            newCode = "existentFunction()"
        )
        println(notFoundResult)
        println()
        
        // Example 8: Handle file that doesn't exist
        println("8. Handle Non-Existent File:")
        println("=" .repeat(40))
        val noFileResult = CodeReplacerUtils.replaceCodeInFile(
            filePath = "/non/existent/file.kt",
            originalCode = "anything",
            newCode = "something"
        )
        println(noFileResult)
        println()
        
        // Example 9: Using detailed result for advanced handling
        println("9. Advanced Result Handling:")
        println("=" .repeat(40))
        val detailedResult = CodeReplacerUtils.replaceCodeSafely(
            filePath = testFile,
            originalCode = "class ExampleClass",
            newCode = "class AdvancedExampleClass",
            options = ReplacementOptions(
                createBackup = false,
                dryRun = true  // Just preview
            )
        )
        
        when (detailedResult) {
            is com.example.mindweaverstudio.data.utils.codereplacer.models.ReplacementResult.DryRun -> {
                println("Would replace ${detailedResult.matchesFound} matches")
                println("Preview content (first 200 chars):")
                println(detailedResult.previewContent.take(200) + "...")
            }
            else -> println("Unexpected result: $detailedResult")
        }
        
        println("\n" + "=" .repeat(50))
        println("Final file content:")
        println("=" .repeat(50))
        println(Files.readString(Paths.get(testFile)))
        
    } finally {
        // Clean up test file and any backup files
        cleanup(testFile)
    }
}

/**
 * Creates a test file with sample Kotlin code.
 */
private fun createTestFile(): String {
    val testContent = """
package com.example.test

import kotlin.collections.*

/**
 * Example class for testing code replacement.
 */
class ExampleClass {
    private val name = "Example"
    
    fun oldMethod() {
        println("Old implementation")
    }
    
    fun anotherMethod(): String {
        return "This method won't be changed"
    }
    
    companion object {
        const val VERSION = "1.0"
    }
}

fun topLevelFunction() {
    println("Top level function")
}
""".trimIndent()
    
    val tempFile = File.createTempFile("test_code_replacer", ".kt")
    tempFile.writeText(testContent)
    return tempFile.absolutePath
}

/**
 * Cleans up test files and backup files.
 */
private fun cleanup(testFile: String) {
    try {
        // Delete main test file
        Files.deleteIfExists(Paths.get(testFile))
        
        // Delete any backup files
        val testDir = File(testFile).parentFile
        testDir.listFiles { file ->
            file.name.contains("test_code_replacer") && file.name.contains("backup")
        }?.forEach { backupFile ->
            backupFile.delete()
        }
        
        println("Cleaned up test files")
    } catch (e: Exception) {
        println("Note: Could not clean up all test files: ${e.message}")
    }
}

/**
 * Additional utility functions for testing the CodeReplacer.
 */
object CodeReplacerTest {
    
    suspend fun testEdgeCases() {
        println("Testing edge cases:")
        println("-" .repeat(30))
        
        val testFile = createTestFile()
        
        try {
            // Test with empty original code
            val emptyOriginalResult = CodeReplacerUtils.replaceCodeInFile(
                filePath = testFile,
                originalCode = "",
                newCode = "something"
            )
            println("Empty original code: $emptyOriginalResult")
            
            // Test with identical codes
            val identicalResult = CodeReplacerUtils.replaceCodeSafely(
                filePath = testFile,
                originalCode = "val name = \"Example\"",
                newCode = "val name = \"Example\"",
                options = ReplacementOptions(allowNoChange = true)
            )
            println("Identical codes: ${identicalResult::class.simpleName}")
            
            // Test with multiline replacement
            val multilineResult = CodeReplacerUtils.replaceCodeInFile(
                filePath = testFile,
                originalCode = "fun oldMethod() {\n        println(\"Old implementation\")\n    }",
                newCode = "fun newMethod() {\n        println(\"Line 1\")\n        println(\"Line 2\")\n        println(\"Line 3\")\n    }"
            )
            println("Multiline replacement: $multilineResult")
            
        } finally {
            cleanup(testFile)
        }
    }
    
    suspend fun performanceTest() {
        println("Performance test:")
        println("-" .repeat(20))
        
        // Create a larger test file
        val largeContent = buildString {
            repeat(1000) { i ->
                appendLine("class TestClass$i {")
                appendLine("    fun method$i() {")
                appendLine("        println(\"Method $i\")")
                appendLine("    }")
                appendLine("}")
                appendLine()
            }
        }
        
        val tempFile = File.createTempFile("large_test", ".kt")
        tempFile.writeText(largeContent)
        
        try {
            val startTime = System.currentTimeMillis()
            
            val result = CodeReplacerUtils.replaceCodeInFile(
                filePath = tempFile.absolutePath,
                originalCode = "println(\"Method 500\")",
                newCode = "println(\"Modified Method 500\")"
            )
            
            val endTime = System.currentTimeMillis()
            println("Result: $result")
            println("Time taken: ${endTime - startTime}ms")
            
        } finally {
            tempFile.delete()
        }
    }
}
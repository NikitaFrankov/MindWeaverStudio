package com.example.mindweaverstudio.data.utils.sourcecode.examples

import com.example.mindweaverstudio.data.utils.sourcecode.SourceCodeUtils
import kotlinx.coroutines.runBlocking

/**
 * Example usage and testing of the SourceCodeFinder utility.
 * 
 * This example demonstrates how to use the SourceCodeFinder to locate and extract
 * source code from a Kotlin project.
 */
fun main() = runBlocking {
    val projectRoot = "/Users/nikitaradionov/IdeaProjects/MindWeaver Studio"
    
    println("=== SourceCodeFinder Example ===")
    println()
    
    // Test 1: Find an interface
    println("1. Finding interface 'Agent':")
    println("=" .repeat(50))
    val agentResult = SourceCodeUtils.findAndFormatSourceCode(projectRoot, "Agent")
    println(agentResult)
    println()
    
    // Test 2: Find a function
    println("2. Finding function 'scanDirectoryToFileNode':")
    println("=" .repeat(50))
    val functionResult = SourceCodeUtils.findAndFormatSourceCode(projectRoot, "scanDirectoryToFileNode")
    println(functionResult)
    println()
    
    // Test 3: Find a class that might have multiple matches
    println("3. Finding class 'AiClient' (might have multiple matches):")
    println("=" .repeat(50))
    val classResult = SourceCodeUtils.findAndFormatSourceCode(projectRoot, "AiClient")
    println(classResult)
    println()
    
    // Test 4: Test non-existent entity
    println("4. Finding non-existent entity 'NonExistentClass':")
    println("=" .repeat(50))
    val notFoundResult = SourceCodeUtils.findAndFormatSourceCode(projectRoot, "NonExistentClass")
    println(notFoundResult)
    println()
    
    // Test 5: Find with package disambiguation
    println("5. Finding 'ChatMessage' with package disambiguation:")
    println("=" .repeat(50))
    val packageResult = SourceCodeUtils.findInPackage(
        projectRoot, 
        "ChatMessage", 
        packageName = "codeeditor.models"
    )
    println(packageResult)
    println()
    
    // Test 6: Get clean source code (without metadata)
    println("6. Getting clean source code for 'Agent':")
    println("=" .repeat(50))
    val cleanSource = SourceCodeUtils.findCleanSourceCode(projectRoot, "Agent")
    if (cleanSource != null) {
        println(cleanSource)
    } else {
        println("Not found or error occurred")
    }
    println()
    
    // Test 7: Check if entity exists
    println("7. Checking if entities exist:")
    println("=" .repeat(30))
    val agentExists = SourceCodeUtils.exists(projectRoot, "Agent")
    val fakeExists = SourceCodeUtils.exists(projectRoot, "FakeClass")
    println("Agent exists: $agentExists")
    println("FakeClass exists: $fakeExists")
    println()
    
    // Test 8: Find all matches for a common name
    println("8. Finding all matches for 'ChatMessage':")
    println("=" .repeat(50))
    val allMatches = SourceCodeUtils.findAllMatches(projectRoot, "ChatMessage")
    println("Found ${allMatches.size} matches:")
    allMatches.forEachIndexed { index, match ->
        println("${index + 1}. ${match.entityType} in ${match.packageName ?: "default package"}")
        println("   File: ${match.filePath}")
        println("   Line: ${match.lineNumber}")
    }
}

/**
 * Additional utility functions for testing the SourceCodeFinder.
 */
object SourceCodeFinderTest {
    
    suspend fun testSpecificEntities(projectRoot: String) {
        val testCases = listOf(
            "Agent" to "interface",
            "scanDirectoryToFileNode" to "function",
            "AiClient" to "interface",
            "CODE_TESTER_AGENT" to "constant",
            "DefaultRootComponent" to "class",
            "EntityType" to "enum class"
        )
        
        println("Testing specific entities:")
        println("=" .repeat(50))
        
        testCases.forEach { (entityName, expectedType) ->
            println("Testing: $entityName (expected: $expectedType)")
            val result = SourceCodeUtils.findCleanSourceCode(projectRoot, entityName)
            if (result != null) {
                println("✅ Found")
                println("First few lines:")
                result.lines().take(5).forEach { line ->
                    if (line.isNotBlank()) println("  $line")
                }
            } else {
                println("❌ Not found")
            }
            println()
        }
    }
    
    suspend fun performanceTest(projectRoot: String) {
        println("Performance test:")
        println("=" .repeat(30))
        
        val startTime = System.currentTimeMillis()
        
        val testEntities = listOf("Agent", "AiClient", "scanDirectoryToFileNode", "NonExistent")
        testEntities.forEach { entityName ->
            SourceCodeUtils.exists(projectRoot, entityName)
        }
        
        val endTime = System.currentTimeMillis()
        println("Time taken for ${testEntities.size} searches: ${endTime - startTime}ms")
    }
}
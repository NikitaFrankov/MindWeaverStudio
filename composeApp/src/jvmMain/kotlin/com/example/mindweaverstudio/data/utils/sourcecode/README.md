# SourceCodeFinder Utility

A comprehensive utility for finding and extracting the full source code of classes, functions, and other entities in Kotlin projects by name.

## Overview

The SourceCodeFinder utility allows you to:
- Find classes, interfaces, objects, functions, and constants by exact name
- Extract complete source code with imports and proper formatting
- Handle multiple matches with package disambiguation
- Search across entire Kotlin projects recursively
- Distinguish between different types of entities (classes, interfaces, functions, etc.)

## Features

✅ **Complete Entity Support**: Classes, interfaces, objects, data classes, sealed classes, enum classes, functions, constants, and properties  
✅ **Package Disambiguation**: Handle multiple entities with the same name in different packages  
✅ **Import Preservation**: Automatically includes all necessary import statements  
✅ **Exact Matching**: Precise name matching to avoid false positives  
✅ **Error Handling**: Clear error messages for not found or invalid scenarios  
✅ **Coroutine Support**: Asynchronous operations with suspend functions  
✅ **File Traversal**: Recursive search through entire project directory structures  

## Quick Start

### Basic Usage

```kotlin
import com.example.mindweaverstudio.data.utils.sourcecode.SourceCodeUtils
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val projectRoot = "/path/to/your/kotlin/project"
    
    // Find and get formatted source code
    val result = SourceCodeUtils.findAndFormatSourceCode(projectRoot, "MyClass")
    println(result)
    
    // Check if an entity exists
    val exists = SourceCodeUtils.exists(projectRoot, "MyFunction")
    println("MyFunction exists: $exists")
    
    // Get clean source code without metadata
    val sourceCode = SourceCodeUtils.findCleanSourceCode(projectRoot, "MyInterface")
    println(sourceCode)
}
```

### Advanced Usage

```kotlin
// Find with package disambiguation
val result = SourceCodeUtils.findInPackage(
    projectRoot = "/path/to/project",
    targetName = "ChatMessage", 
    packageName = "models"
)

// Get all matches for entities with the same name
val matches = SourceCodeUtils.findAllMatches(projectRoot, "Handler")
matches.forEach { match ->
    println("${match.entityType} in ${match.packageName}")
    println("File: ${match.filePath}, Line: ${match.lineNumber}")
}

// Detailed search results
val searchResult = SourceCodeUtils.findSourceCodeDetailed(projectRoot, "MyClass")
when (searchResult) {
    is SearchResult.Success -> println("Found single match")
    is SearchResult.MultipleMatches -> println("Found ${searchResult.matches.size} matches")
    is SearchResult.NotFound -> println("Not found")
    is SearchResult.Error -> println("Error: ${searchResult.message}")
}
```

## API Reference

### Main Functions

#### `findAndFormatSourceCode(projectRoot, targetName, exactMatch = true)`
Returns formatted source code with metadata comments, imports, and the entity source.

**Parameters:**
- `projectRoot`: Path to the root directory of the Kotlin project
- `targetName`: Name of the class or function to find
- `exactMatch`: If true, searches for exact name match only (default: true)

**Returns:** Formatted string with source code or error message

#### `findCleanSourceCode(projectRoot, targetName, exactMatch = true)`
Returns clean source code with imports but without metadata comments.

**Returns:** Clean source code string or null if not found

#### `exists(projectRoot, targetName, exactMatch = true)`
Checks if a class or function exists in the project.

**Returns:** Boolean indicating whether the entity exists

#### `findAllMatches(projectRoot, targetName, exactMatch = true)`
Finds all matches for a target name and returns them as a list.

**Returns:** List of `SourceCodeMatch` objects

#### `findInPackage(projectRoot, targetName, packageName, exactMatch = true)`
Finds entities with package disambiguation.

**Parameters:**
- `packageName`: Package name to narrow down the search (optional)

**Returns:** Formatted source code or error message

#### `findSourceCodeDetailed(projectRoot, targetName, exactMatch = true)`
Returns detailed search results with full metadata.

**Returns:** `SearchResult` sealed class instance

### Data Models

#### `SourceCodeMatch`
```kotlin
data class SourceCodeMatch(
    val entityName: String,        // Name of the found entity
    val entityType: EntityType,    // Type of entity (CLASS, FUNCTION, etc.)
    val sourceCode: String,        // Complete source code
    val filePath: String,          // Path to the file containing the entity
    val packageName: String?,      // Package name (null for default package)
    val lineNumber: Int,           // Line number where entity starts
    val imports: List<String>      // List of import statements in the file
)
```

#### `EntityType` (Enum)
- `CLASS` - Regular classes
- `INTERFACE` - Interfaces
- `OBJECT` - Object declarations
- `DATA_CLASS` - Data classes
- `SEALED_CLASS` - Sealed classes
- `ENUM_CLASS` - Enum classes
- `FUNCTION` - Top-level and member functions
- `CONSTANT` - const val declarations
- `PROPERTY` - val/var declarations
- `ANNOTATION_CLASS` - Annotation classes

#### `SearchResult` (Sealed Class)
- `Success(matches)` - Single match found
- `MultipleMatches(matches)` - Multiple matches found
- `NotFound(targetName, searchedFiles)` - Entity not found
- `Error(message, cause)` - Error occurred during search

## Example Output

### Single Match
```kotlin
// Found: Agent (interface)
// File: /path/to/project/data/models/agents/Agent.kt
// Line: 10
// Package: com.example.mindweaverstudio.data.models.agents

package com.example.mindweaverstudio.data.models.agents

import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage

interface Agent {
    val name: String
    val description: String
    suspend fun run(input: ChatMessage): AgentResult
}
```

### Multiple Matches
```kotlin
Multiple matches found for 'ChatMessage':

--- Match 1 ---
Type: data class
Package: com.example.mindweaverstudio.components.codeeditor.models
File: /path/to/ChatMessage.kt
Line: 5

package com.example.mindweaverstudio.components.codeeditor.models

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

--- Match 2 ---
Type: data class
Package: com.example.mindweaverstudio.data.models.chat
File: /path/to/other/ChatMessage.kt
Line: 3

package com.example.mindweaverstudio.data.models.chat

data class ChatMessage(
    val role: String,
    val content: String
)
```

## Supported Kotlin Constructs

The utility can find and extract:

### Classes and Objects
- Regular classes: `class MyClass`
- Data classes: `data class User`
- Sealed classes: `sealed class Result`
- Enum classes: `enum class Color`
- Object declarations: `object Singleton`
- Annotation classes: `annotation class MyAnnotation`

### Interfaces
- Regular interfaces: `interface Repository`

### Functions
- Top-level functions: `fun calculateSum()`
- Suspend functions: `suspend fun fetchData()`
- Inline functions: `inline fun <reified T> genericFunc()`
- Operator functions: `operator fun plus()`
- Infix functions: `infix fun String.append()`

### Properties and Constants
- Constants: `const val API_KEY = "..."`
- Properties: `val user: User`
- Properties with getters: `val name: String get() = ...`

### Visibility Modifiers
All visibility modifiers are supported:
- `public` (default)
- `private`
- `internal`
- `protected`

## Error Handling

The utility provides comprehensive error handling:

```kotlin
when (val result = SourceCodeUtils.findSourceCodeDetailed(projectRoot, "MyClass")) {
    is SearchResult.Success -> {
        // Handle single match
    }
    is SearchResult.MultipleMatches -> {
        // Handle disambiguation needed
    }
    is SearchResult.NotFound -> {
        println("Entity '${result.targetName}' not found in ${result.searchedFiles} files")
    }
    is SearchResult.Error -> {
        println("Search error: ${result.message}")
        result.cause?.printStackTrace()
    }
}
```

## Performance Considerations

- **File Caching**: The utility reads files on-demand but doesn't cache results between searches
- **Large Projects**: Tested with projects containing thousands of Kotlin files
- **Memory Usage**: Minimal memory footprint - only loads files currently being analyzed
- **Parallel Processing**: Searches are performed sequentially for stability

## Limitations

1. **Preprocessing**: Does not handle Kotlin preprocessor directives or build-time generation
2. **Complex Generics**: May not perfectly extract complex generic type declarations
3. **Nested Classes**: Finds nested classes but extracts the entire outer class source
4. **Reflection**: Does not use Kotlin reflection - relies on text parsing

## Project Structure

```
sourcecode/
├── SourceCodeFinder.kt          # Main finder implementation
├── SourceCodeFormatter.kt       # Output formatting
├── SourceCodeUtils.kt           # Public API
├── models/
│   ├── SourceCodeMatch.kt       # Data model for matches
│   └── SearchResult.kt          # Result types
└── examples/
    └── SourceCodeFinderExample.kt  # Usage examples
```

## Integration

To integrate this utility into your project:

1. Copy the entire `sourcecode` package to your project
2. Import `SourceCodeUtils` in your code
3. Use the suspend functions within coroutine scopes

```kotlin
import com.example.mindweaverstudio.data.utils.sourcecode.SourceCodeUtils
```

## License

This utility is part of the MindWeaver Studio project and follows the same licensing terms.
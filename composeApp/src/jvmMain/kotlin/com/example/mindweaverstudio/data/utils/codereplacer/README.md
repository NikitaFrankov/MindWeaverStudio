# CodeReplacer Utility

A comprehensive utility for safely replacing specific fragments of source code within files while preserving the rest of the file structure, formatting, and imports.

## Overview

The CodeReplacer utility provides precise code replacement capabilities with:
- **Exact matching**: Finds and replaces code fragments with exact string matching
- **File safety**: Atomic operations with optional backup creation
- **Structure preservation**: Maintains formatting, imports, and file structure
- **Error handling**: Comprehensive error reporting and validation
- **Multiple match handling**: Options for handling multiple occurrences
- **Preview functionality**: Dry-run capabilities to preview changes

## Quick Start

### Basic Usage

```kotlin
import com.example.mindweaverstudio.data.utils.codereplacer.CodeReplacerUtils
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Basic code replacement
    val result = CodeReplacerUtils.replaceCodeInFile(
        filePath = "/path/to/your/file.kt",
        originalCode = "fun oldMethod() {\n    println(\"old\")\n}",
        newCode = "fun newMethod() {\n    println(\"new\")\n}"
    )
    println(result)
    
    // Preview changes before applying
    val preview = CodeReplacerUtils.previewReplacement(
        filePath = "/path/to/your/file.kt",
        originalCode = "val name = \"old\"",
        newCode = "val name = \"new\""
    )
    println(preview)
}
```

### Advanced Usage

```kotlin
// Replace with backup and custom options
val result = CodeReplacerUtils.replaceCodeWithOptions(
    filePath = "/path/to/file.kt",
    originalCode = "deprecated code",
    newCode = "improved code",
    createBackup = true,
    allowMultiple = true
)

// Multiple replacements in one operation
val replacements = listOf(
    "oldFunction()" to "newFunction()",
    "DEPRECATED_CONSTANT" to "NEW_CONSTANT"
)
val multiResult = CodeReplacerUtils.replaceMultipleFragments(
    filePath = "/path/to/file.kt",
    replacements = replacements,
    createBackup = true
)

// Get detailed replacement information
val detailedResult = CodeReplacerUtils.replaceCodeSafely(
    filePath = "/path/to/file.kt",
    originalCode = "code to replace",
    newCode = "replacement code",
    options = ReplacementOptions(
        createBackup = true,
        allowMultipleMatches = false,
        dryRun = false
    )
)
```

## Features

✅ **Exact String Matching**: Finds code fragments with precise string matching  
✅ **Atomic Operations**: All file operations are atomic (succeed completely or not at all)  
✅ **Backup Creation**: Optional automatic backup before modifications  
✅ **Multiple Match Handling**: Options for handling multiple occurrences of the same code  
✅ **Dry Run Capability**: Preview changes without modifying files  
✅ **File Structure Preservation**: Maintains imports, formatting, and overall structure  
✅ **Comprehensive Error Handling**: Clear error messages for all failure scenarios  
✅ **Coroutine Support**: Asynchronous operations with suspend functions  
✅ **Line Ending Normalization**: Handles different line ending formats consistently  

## API Reference

### Main Functions

#### `replaceCodeInFile(filePath, originalCode, newCode)`
Basic code replacement with default options.

**Parameters:**
- `filePath`: Path to the source file
- `originalCode`: Code fragment to be replaced
- `newCode`: Replacement code

**Returns:** String message indicating success or failure

#### `replaceCodeWithOptions(filePath, originalCode, newCode, createBackup, allowMultiple)`
Code replacement with basic customization options.

**Parameters:**
- `createBackup`: Whether to create a backup file (default: false)
- `allowMultiple`: Whether to replace multiple matches (default: false)

#### `previewReplacement(filePath, originalCode, newCode)`
Shows what would be changed without making modifications.

**Returns:** String description of planned changes

#### `replaceCodeSafely(filePath, originalCode, newCode, options)`
Full-featured replacement with comprehensive options.

**Parameters:**
- `options`: ReplacementOptions object with detailed configuration

**Returns:** ReplacementResult with detailed information

#### `findCodeFragment(filePath, codeFragment)`
Searches for a code fragment without replacing it.

**Returns:** String indicating whether the code was found

#### `replaceMultipleFragments(filePath, replacements, createBackup)`
Replaces multiple code fragments in a single file operation.

**Parameters:**
- `replacements`: List of (originalCode, newCode) pairs

#### `getReplacementPreview(filePath, originalCode, newCode)`
Gets detailed preview information with context.

**Returns:** Detailed preview with before/after context

### Data Models

#### `ReplacementOptions`
Configuration options for replacement operations:

```kotlin
data class ReplacementOptions(
    val exactMatch: Boolean = true,                    // Require exact string match
    val preserveWhitespace: Boolean = true,            // Preserve original whitespace
    val normalizeLineEndings: Boolean = true,          // Normalize line endings
    val allowMultipleMatches: Boolean = false,         // Allow multiple matches
    val allowNoChange: Boolean = false,                // Allow identical original/new code
    val createBackup: Boolean = false,                 // Create backup before changes
    val validateSyntax: Boolean = false,               // Validate syntax after replacement
    val dryRun: Boolean = false                        // Only simulate, don't write
)
```

#### `ReplacementResult` (Sealed Class)
Detailed result information:

- `Success(filePath, matchesFound, matchesReplaced, backupPath, message)` - Successful replacement
- `NotFound(filePath, originalCode, searchedLines)` - Code fragment not found
- `MultipleMatches(filePath, matches, message)` - Multiple matches found but not allowed
- `Error(message, cause, filePath)` - Error during operation
- `DryRun(filePath, matchesFound, matches, previewContent, message)` - Preview results
- `NoChange(filePath, message)` - No changes made (identical codes)

#### `ReplacementMatch`
Information about found matches:

```kotlin
data class ReplacementMatch(
    val startLine: Int,              // Starting line number (1-based)
    val endLine: Int,                // Ending line number (1-based)
    val startColumn: Int,            // Starting column (1-based)
    val endColumn: Int,              // Ending column (1-based)
    val matchedText: String,         // The actual matched text
    val contextBefore: String,       // Context lines before match
    val contextAfter: String         // Context lines after match
)
```

## Usage Examples

### Example 1: Simple Function Replacement
```kotlin
val result = CodeReplacerUtils.replaceCodeInFile(
    filePath = "MyClass.kt",
    originalCode = """
        fun calculate(x: Int): Int {
            return x * 2
        }
    """.trimIndent(),
    newCode = """
        fun calculate(x: Int): Int {
            return x * 3 + 1
        }
    """.trimIndent()
)
```

### Example 2: Replace with Backup
```kotlin
val result = CodeReplacerUtils.replaceCodeWithOptions(
    filePath = "ImportantFile.kt",
    originalCode = "const val VERSION = \"1.0\"",
    newCode = "const val VERSION = \"2.0\"",
    createBackup = true,
    allowMultiple = false
)
```

### Example 3: Preview Before Replace
```kotlin
val preview = CodeReplacerUtils.getReplacementPreview(
    filePath = "MyClass.kt",
    originalCode = "private val logger = Logger.getLogger(\"old\")",
    newCode = "private val logger = Logger.getLogger(\"new\")"
)
println(preview)
```

### Example 4: Multiple Replacements
```kotlin
val replacements = listOf(
    "TODO(\"implement this\")" to "// Implementation complete",
    "deprecated_function()" to "new_function()",
    "OLD_CONSTANT" to "NEW_CONSTANT"
)

val result = CodeReplacerUtils.replaceMultipleFragments(
    filePath = "LegacyCode.kt",
    replacements = replacements,
    createBackup = true
)
```

## Error Handling

The utility provides comprehensive error handling for various scenarios:

```kotlin
val result = CodeReplacerUtils.replaceCodeSafely(
    filePath = "MyFile.kt",
    originalCode = "code to replace",
    newCode = "new code",
    options = ReplacementOptions(createBackup = true)
)

when (result) {
    is ReplacementResult.Success -> {
        println("✅ Success: ${result.message}")
        if (result.backupPath != null) {
            println("Backup created: ${result.backupPath}")
        }
    }
    
    is ReplacementResult.NotFound -> {
        println("❌ Code not found in ${result.filePath}")
    }
    
    is ReplacementResult.MultipleMatches -> {
        println("⚠️ Multiple matches found:")
        result.matches.forEach { match ->
            println("  - ${match.getLocationDescription()}")
        }
    }
    
    is ReplacementResult.Error -> {
        println("❌ Error: ${result.message}")
        result.cause?.printStackTrace()
    }
    
    is ReplacementResult.DryRun -> {
        println("📋 Preview: Would replace ${result.matchesFound} matches")
    }
    
    is ReplacementResult.NoChange -> {
        println("ℹ️ No changes: ${result.message}")
    }
}
```

## Common Error Scenarios

1. **File Not Found**: Clear error message when the specified file doesn't exist
2. **Code Not Found**: Informative message when the original code fragment isn't found
3. **Multiple Matches**: Detailed information about all matches when `allowMultipleMatches` is false
4. **Permission Errors**: Proper handling of file permission issues
5. **I/O Errors**: Graceful handling of disk space or network file system issues
6. **Invalid Parameters**: Validation of input parameters with helpful error messages

## Safety Features

### Atomic Operations
All file modifications are atomic - either the entire operation succeeds or the file remains unchanged.

### Backup Creation
Optional automatic backup creation before any modifications:
```kotlin
val options = ReplacementOptions(createBackup = true)
```

### Dry Run Mode
Preview changes without modifying files:
```kotlin
val options = ReplacementOptions(dryRun = true)
```

### Validation
Input validation prevents common errors:
- Empty file paths
- Blank original code
- Identical original and new code (unless allowed)

## Performance Characteristics

- **File Reading**: Files are read once per operation
- **Memory Usage**: Minimal memory footprint for large files
- **Pattern Matching**: Efficient string matching algorithm
- **Atomic Writes**: Temporary file approach for safety

## Best Practices

1. **Always Preview First**: Use `previewReplacement()` for important changes
2. **Create Backups**: Use `createBackup = true` for critical files
3. **Handle Multiple Matches**: Consider carefully whether to allow multiple replacements
4. **Exact Matching**: Ensure your original code fragment exactly matches the file content
5. **Test on Copies**: Test replacements on file copies before applying to originals

## Integration

To use this utility in your project:

```kotlin
import com.example.mindweaverstudio.data.utils.codereplacer.CodeReplacerUtils

// Simple replacement
val result = CodeReplacerUtils.replaceCodeInFile(filePath, original, new)

// Advanced usage
val detailedResult = CodeReplacerUtils.replaceCodeSafely(
    filePath, original, new, 
    ReplacementOptions(createBackup = true)
)
```

## Limitations

1. **Exact Matching Only**: Currently supports exact string matching only
2. **Single File Operations**: Designed for single file operations (use multiple calls for multiple files)
3. **Text Files Only**: Designed for text-based source files
4. **Memory Usage**: Large files are loaded entirely into memory during processing

## Project Structure

```
codereplacer/
├── CodeReplacer.kt              # Core replacement logic
├── CodeReplacerUtils.kt         # Public API
├── models/
│   ├── ReplacementRequest.kt    # Request data model
│   └── ReplacementResult.kt     # Result types and matches
└── examples/
    └── CodeReplacerExample.kt   # Usage examples and tests
```

## Future Enhancements

Potential future improvements:
- Fuzzy matching capabilities
- Regex pattern support  
- Syntax-aware replacements
- Bulk file operations
- Integration with version control systems
- Undo/redo functionality

## License

This utility is part of the MindWeaver Studio project and follows the same licensing terms.
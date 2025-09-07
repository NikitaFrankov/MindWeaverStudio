# RAG Code Chunking Utility

A comprehensive utility for intelligently chunking source code repositories into semantic units optimized for Retrieval-Augmented Generation (RAG) pipelines.

## 🚀 **Truly Streaming Version - Zero Memory Accumulation!**

**For large repositories that cause OutOfMemoryError, use the new [Truly Streaming Solution](TRULY_STREAMING_SOLUTION.md) that processes chunks one by one without ANY intermediate memory accumulation.**

```kotlin
// Recommended for ALL repositories, especially large ones
val utility = StreamingRAGChunkingUtility.createMemoryOptimized()
utility.processRepositoryStreaming("./any-size-repo", "./output")
// Works with repositories of unlimited size!
```

The truly streaming version solves the final memory issue where even the "streaming" approach was creating chunk lists per file.

## Features

### ⚡ Semantic Chunking
- **Intelligent Code Units**: Chunks represent meaningful semantic units (classes, interfaces, enums, methods, functions, complex properties)
- **Large Element Splitting**: Automatically splits large code elements (>200 lines) into logical sub-chunks based on control flow structures (if/else, loops, try/catch)
- **Signature Preservation**: Always includes class/method signatures in sub-chunks for context

### 🎯 Context Preservation
- **Package & Imports**: Each chunk includes package declarations and essential imports
- **Class Context**: Methods include their containing class signature
- **Related Methods**: Private methods related to the current chunk are included as references
- **Documentation**: JavaDoc/KDoc comments are preserved with their corresponding code

### 📏 Size Management
- **Configurable Limits**: Maximum chunk size of 300 lines (~3000 tokens) by default
- **Smart Overlaps**: 5-10 line overlaps between adjacent chunks when needed
- **Token Estimation**: Rough token count estimation for each chunk

### 📊 Rich Metadata
Each chunk includes comprehensive metadata:
- File path, class name, method name
- Start and end line numbers
- Chunk type and content
- Token count estimation
- Contextual information (package, imports, signatures)
- Overlap indicators

## Architecture

```
RAGChunkingUtility/
├── models/
│   └── ChunkMetadata.kt        # Data structures for chunks and metadata
├── parser/
│   └── KotlinCodeParser.kt     # Kotlin/Java code semantic parser
├── chunker/
│   └── SemanticChunker.kt      # Core chunking logic with semantic awareness
├── scanner/
│   └── RepositoryScanner.kt    # Repository scanning and file filtering
├── output/
│   └── JsonExporter.kt         # JSON export for RAG pipelines
└── examples/
    └── ChunkingExample.kt      # Usage examples and demos
```

## Quick Start

### Basic Usage

```kotlin
// Simplest usage - chunk current project
RAGChunkingUtility.chunkCurrentProject(
    outputDir = "./rag_chunks",
    includeTests = false
)
```

### Custom Configuration

```kotlin
val utility = RAGChunkingUtility.create(
    maxChunkSize = 250,           // Maximum lines per chunk
    largeElementThreshold = 150,   // When to split large elements
    overlapSize = 10              // Lines of overlap between chunks
)

val chunks = utility.chunkRepository(
    repositoryPath = "./my-project",
    includeTests = false
)

// Export for RAG pipeline
utility.exportForRAG(chunks, "./rag_output.json", batchSize = 1000)
```

### Processing Specific Files/Directories

```kotlin
val utility = RAGChunkingUtility()

// Single file
val fileChunks = utility.chunkFile("./src/MyClass.kt")

// Specific directory
val directoryChunks = utility.chunkDirectory("./src/ai", includeTests = false)

// Get statistics
val stats = utility.getStatistics(chunks)
println("Total chunks: ${stats.totalChunks}")
println("Average tokens per chunk: ${stats.averageTokensPerChunk}")
```

## Configuration Options

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxChunkSize` | 300 | Maximum lines per chunk |
| `largeElementThreshold` | 200 | Line count threshold for splitting large elements |
| `overlapSize` | 7 | Number of overlapping lines between adjacent chunks |

## Output Formats

### Standard JSON Export
```kotlin
utility.exportChunks(chunks, "./chunks.json", includeMetadata = true)
```

### RAG-Optimized Export (Batched)
```kotlin
utility.exportForRAG(chunks, "./rag_chunks.json", batchSize = 1000)
// Creates: rag_chunks_batch_1.json, rag_chunks_batch_2.json, etc.
```

### Statistics Export
```kotlin
utility.exportStatistics(chunks, "./statistics.json")
```

## Chunk Types

- **CLASS**: Complete class definitions
- **INTERFACE**: Interface definitions  
- **ENUM**: Enum class definitions
- **FUNCTION**: Method/function definitions
- **PROPERTY**: Complex property definitions with custom getters/setters
- **SUB_CHUNK**: Sub-sections of large elements split by logical blocks

## File Filtering

**Included:**
- `.kt` (Kotlin files)
- `.java` (Java files)

**Excluded Directories:**
- `build`, `target`, `.gradle`, `.idea`, `.git`
- `node_modules`, `.vscode`, `out`, `bin`
- `generated`, `resources`

**Test File Handling:**
- Configurable inclusion/exclusion of test files
- Auto-detects test files by path and naming conventions

## Examples

### Complete Workflow
```kotlin
val utility = RAGChunkingUtility()

// Process entire repository
utility.processRepository(
    repositoryPath = "./my-project",
    outputBasePath = "./output/chunks",
    includeTests = false,
    exportForRAG = true
)

// Creates:
// - ./output/chunks_chunks.json (all chunks)
// - ./output/chunks_statistics.json (analysis)
// - ./output/chunks_rag_batch_*.json (RAG-optimized)
```

### Data Analysis
```kotlin
val chunks = utility.chunkRepository("./project")

// Filter by type
val classChunks = chunks.filter { it.chunkType == ChunkType.CLASS }
val methodChunks = chunks.filter { it.chunkType == ChunkType.FUNCTION }

// Find large chunks
val largeChunks = chunks.filter { it.tokens > 500 }

// Analyze by file
val chunksByFile = chunks.groupBy { it.filePath }
```

## Integration with RAG Pipelines

The utility generates JSON output optimized for RAG systems:

```json
{
  "batchId": 1,
  "totalBatches": 3,
  "documents": [
    {
      "id": "DefaultSidebarComponent_class_9-24",
      "content": "package com.example...\n\nclass DefaultSidebarComponent(...) { ... }",
      "metadata": {
        "filePath": "./src/DefaultSidebarComponent.kt",
        "className": "DefaultSidebarComponent",
        "chunkType": "CLASS",
        "startLine": "9",
        "endLine": "24",
        "tokens": "156"
      }
    }
  ]
}
```

## Testing

Run the included tests to validate functionality:

```kotlin
RAGChunkingTest.runTests()
```

Tests cover:
- Basic functionality
- Single file processing  
- Error handling
- Configuration options
- JSON export/import
- Different Kotlin constructs parsing

## Performance Considerations

- **Memory Usage**: Processes files individually to minimize memory footprint
- **Parallel Processing**: Scanner processes files sequentially but can be enhanced for parallel processing
- **Large Repositories**: Use directory-based chunking for very large codebases
- **Batch Export**: RAG export automatically batches large chunk sets

## Limitations

- Currently supports Kotlin and Java only
- Parsing is regex-based, not AST-based (simpler but less precise)
- Token estimation is approximate (4 chars per token)
- No incremental processing (full re-scan required for updates)

## Future Enhancements

- AST-based parsing for higher accuracy
- Support for additional languages (Python, TypeScript, etc.)
- Incremental chunking for large repositories
- Semantic similarity-based chunk grouping
- Integration with vector databases
- Parallel processing for better performance

## License

This utility is part of the MindWeaver Studio project.
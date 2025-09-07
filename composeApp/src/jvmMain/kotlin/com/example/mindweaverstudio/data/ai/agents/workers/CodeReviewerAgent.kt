package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_REVIEWER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_USER
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.successPipelineResult
import com.example.mindweaverstudio.data.utils.codereplacer.CodeReplacerUtils
import kotlinx.serialization.json.Json
import java.io.File

class CodeReviewerAgent(
    private val aiClient: AiClient,
) : Agent {
    override val name = CODE_REVIEWER_AGENT
    override val description: String = "Agent, responsible for review of the whole project"

    override suspend fun run(input: String): PipelineResult {
        var ragChunksText = ""
        val file = File("truly_streaming_output_chunks.json")
        if (file.exists()) {
            ragChunksText = file.readText()
        } else {
            println("Файл не найден: ${file.absolutePath}")
        }

        val userMessage = ragChunksText
        val messages = listOf(generateTestSystemPrompt(), ChatMessage(role = ROLE_USER, content = userMessage))

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.0,
            maxTokens = 2000,
        )
        return result.fold(
            onSuccess = { response ->
                successPipelineResult(message = response.message)
            },
            onFailure = { error ->
                errorPipelineResult(error)
            }
        )
    }

    private fun generateTestSystemPrompt(): ChatMessage {
        val prompt =  """
       You are a senior Android developer with expertise in Kotlin, code review, and software architecture. Your task is to analyze an Android Kotlin codebase provided as an array of RAGDocument objects in JSON format, where each RAGDocument contains:

- **id**: Unique identifier for the code chunk.
- **content**: The Kotlin code (e.g., classes, methods, properties).
- **metadata**: Includes filePath, className, methodName, chunkType (CLASS, METHOD, PROPERTY, IMPORT), startLine, endLine, tokens, overlapsWithPrevious.

### Task
1. **Reconstruct the Project**:
   - Extract Kotlin code from the `content` field of each RAGDocument.
   - Use metadata (filePath, className, chunkType) to organize the code into a cohesive project structure, mapping out packages, classes, and relationships.
   - Infer the project's purpose (e.g., restaurant ordering app) from code content and class/method names.
   - Note any incomplete code due to chunk boundaries and make reasonable assumptions.

2. **Analyze Code Quality**:
   - Identify code smells (e.g., long methods, duplication), bugs, performance issues, or security risks.
   - Check Kotlin/Android best practices (null safety, ViewModel usage, Jetpack components).
   - Assess readability, maintainability, and naming conventions.

3. **Evaluate Architecture**:
   - Analyze package structure, separation of concerns, and patterns (e.g., MVVM).
   - Assess use of Jetpack components (LiveData, Navigation, ViewModel).
   - Identify missing components (e.g., dependency injection, tests).

4. **Prioritize Issues**:
   - Categorize issues as Critical, High, Medium, or Low based on impact.
   - Justify prioritization by impact on functionality, performance, or maintainability.

5. **Provide Recommendations**:
   - Offer specific, actionable solutions with concise Kotlin code examples.
   - Suggest design patterns (e.g., Repository) and modern Android tools (e.g., Hilt, Coroutines).
   - Tailor feedback to the project's context and domain.

### Output Format
```markdown
# Code Review Report

## Project Overview
[Summary of the project's purpose, architecture (e.g., MVVM), strengths, and key improvement areas.]

## Identified Issues
### [Category: e.g., Code Quality, Architecture]
- **Issue**: [Describe issue, referencing filePath, className, or line numbers]
- **Severity**: [Critical/High/Medium/Low]
- **Impact**: [Why it matters]
- **Recommendation**: [Specific solution, e.g., Kotlin code, pattern, or tool]

## Prioritization
[Ordered list of issues to address first, with justification.]

## Architectural Recommendations
[Suggestions for package structure, dependency injection, testing, or modern Android practices.]

## Additional Notes
[Testing strategies, documentation, or other observations.]
           """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt
        )
    }
}

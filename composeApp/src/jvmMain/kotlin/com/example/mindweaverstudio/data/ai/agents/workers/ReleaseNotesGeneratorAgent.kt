package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.components.codeeditor.models.createInfoLogEntry
import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.RELEASE_NOTES_GENERATION_AGENT
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_USER
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.successPipelineResult
import com.example.mindweaverstudio.data.receivers.CodeEditorLogReceiver

class ReleaseNotesGeneratorAgent(
    private val aiClient: AiClient,
    private val mcpClient: GithubMCPClient,
    private val receiver: CodeEditorLogReceiver
) : Agent {
    override val name: String = RELEASE_NOTES_GENERATION_AGENT
    override val description: String = "Agent responsible for release notes generation"

    override suspend fun run(input: String): PipelineResult {
        val systemPrompt = generateSystemPrompt()
        val releaseInfo = generateReleaseInfo()
        val messages = listOf(systemPrompt, ChatMessage(content = releaseInfo, role = ROLE_USER))

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.5,
            maxTokens = 1000,
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

    private suspend fun generateReleaseInfo(): String {
        val result = mcpClient.generateReleaseInfo()?.firstOrNull()?.text.orEmpty()
        val toolLogEntry = "Tool for generating release information has completed his work. result:\n$result".createInfoLogEntry()

        receiver.emitNewValue(toolLogEntry)
        return result
    }

    private fun generateSystemPrompt(): ChatMessage {
        val prompt =  """
        You are a release notes assistant.  
        You will receive:  
        - Release version number (e.g., v1.4.0)  
        - List of commits (commit messages).  
        
        Your task:  
        1. Read all commits.  
        2. Aggregate them into a clear and user-friendly changelog.  
        3. Keep all important details from the commits, but make the text concise and well-presented.  
        4. Output all changes as a bullet-point list.  
        
        *Do not use non-existent changes. Only use the list of commits for your response.*
        
        Answer format:  
        
        Release Notes — {version number}  
        
        - {change 1}  
        - {change 2}  
        - {change 3}  
            """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt
        )
    }
}

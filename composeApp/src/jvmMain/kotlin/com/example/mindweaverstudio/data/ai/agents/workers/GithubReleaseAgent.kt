package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.components.codeeditor.models.createInfoLogEntry
import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.GITHUB_RELEASE_AGENT
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_USER
import com.example.mindweaverstudio.data.models.mcp.base.ToolCall
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.successPipelineResult
import com.example.mindweaverstudio.data.receivers.CodeEditorLogReceiver
import kotlinx.serialization.json.Json

class GithubReleaseAgent(
    private val aiClient: AiClient,
    private val mcpClient: GithubMCPClient,
    private val receiver: CodeEditorLogReceiver
) : Agent {
    override val name: String = GITHUB_RELEASE_AGENT
    override val description: String = "Agent responsible for create release on github.com"

    override suspend fun run(input: String): PipelineResult {
        val systemPrompt = generateSystemPrompt()
        val messages = listOf(systemPrompt, ChatMessage(content = input, role = ROLE_USER))

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.2,
            maxTokens = 1500,
        )
        return result.fold(
            onSuccess = { response ->
                handleToolCall(message = response.message)
            },
            onFailure = { error ->
                errorPipelineResult(error)
            }
        )
    }

    private suspend fun handleToolCall(message: String): PipelineResult {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        try {
            val toolCall = json.decodeFromString(ToolCall.serializer(), message)
            val result = mcpClient.callTool(toolCall)?.firstOrNull()?.text.orEmpty()
            val toolLogEntry = "Tool for creating github release has completed his work. result:\n$result".createInfoLogEntry()

            receiver.emitNewValue(toolLogEntry)

            return successPipelineResult(result)
        } catch (e: Exception) {
            return errorPipelineResult(e)
        }
    }

    private suspend fun generateSystemPrompt(): ChatMessage {
        val tools = mcpClient.getTools()
        val prompt =  """
            You are an AI agent integrated with a mcp server
            You use only **ONE** tool - for create github release.  
            You must follow these rules strictly:
            
            1. You are given a list of tools, but You use only **ONE** tool - for create github release. :
                $tools
            
            2. When the user makes a request related to generating information about the next release,
                you must respond only with a JSON object of the following form:
            
               {
                 "action": "call_tool",
                 "tool": "<tool_name>",
                 "params": { <key-value pairs> }
               }
            
            3. Never include explanations, natural language, comments, or any additional text.  
               Your output must always be a **valid JSON object only**, parsable by standard JSON parsers.  
            
            4. If the request cannot be mapped to any available tool, respond with:
            
               {
                 "action": "no_tool",
                 "reason": "<short machine-readable reason>"
               }
            
            5. Do not invent tools, parameters, or functionality.  
               Only use the provided tool list.  
            
            6. Preserve the integrity of code and text passed as parameters (do not escape or reformat unnecessarily).  
            
            This system prompt establishes a strict contract:  
            You are a JSON-only tool invocation layer.  
            No additional conversation, no reasoning, no markdown formatting.  
            """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt
        )
    }
}

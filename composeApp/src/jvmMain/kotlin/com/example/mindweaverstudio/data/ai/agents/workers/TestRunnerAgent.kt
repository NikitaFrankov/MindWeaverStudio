package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.components.codeeditor.models.createInfoLogEntry
import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.mcp.DockerMCPClient
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createErrorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createSuccessPipelineResult
import com.example.mindweaverstudio.data.ai.agents.TEST_RUNNER_AGENT
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage.Companion.ROLE_USER
import com.example.mindweaverstudio.data.models.mcp.base.ToolCall
import kotlinx.serialization.json.Json

class TestRunnerAgent(
    private val aiClient: AiClient,
    private val dockerMCPClient: DockerMCPClient,
) : Agent {
    override val name: String = TEST_RUNNER_AGENT
    override val description: String = "Агент, который запускает созданные JUnit тесты в докер контейнере. На вход подаются тесты и путь к файлу, для которого эти тесты писались"

    override suspend fun run(input: String): PipelineResult {
        val systemPrompt = generateTestSystemPrompt()
        val messages = listOf(systemPrompt, ChatMessage(input, ROLE_USER))

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.3,
            maxTokens = 1000,
        )
        return result.fold(
            onSuccess = { response ->
                createSuccessPipelineResult(message = response.message)
            },
            onFailure = { error ->
                createErrorPipelineResult(error)
            }
        )
    }

    private suspend fun handleRunDockerWithCodeToolCall(
        message: String,
        filePath: String,
    ) {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val toolCall = json.decodeFromString(ToolCall.serializer(), message)
        val currentToolCall = toolCall.copy(
            params = buildMap {
                putAll(toolCall.params)
                replace("file_path", "state().project.path" + filePath)
            }
        )

        val toolLogEntry = "Tests created. Start process to check tests".createInfoLogEntry()

        val result: String = dockerMCPClient.callTool(currentToolCall)?.firstOrNull()?.text.orEmpty()

        // fetchRootNode(filePath = state().project.path)
    }

    private suspend fun generateTestSystemPrompt(): ChatMessage {
        val tools = dockerMCPClient.getTools()
        val prompt =  """
            You are an AI agent integrated with a mcp server.  
            You must follow these rules strictly:
            
            1. You are given a list of tools:
                $tools
            
            2. When the user makes a request that can be satisfied by one of the tools,  
               you must respond **only** with a JSON object of the following form:
            
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

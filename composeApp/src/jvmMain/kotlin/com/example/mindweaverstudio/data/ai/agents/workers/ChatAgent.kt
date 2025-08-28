package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createErrorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createSuccessPipelineResult
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.mcp.ThinkMcpClient
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_ASSISTANT
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_USER
import com.example.mindweaverstudio.data.models.mcp.base.ToolCall
import kotlinx.serialization.json.Json


class ChatAgent(
    private val thinkMcpClient: ThinkMcpClient,
    private val aiClient: AiClient,
) : Agent {
    override val name = CHAT_AGENT
    override val description: String = "Агент для обычного общения"
    private var history: List<ChatMessage> = emptyList()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private var isThink = false

    override suspend fun run(input: String): PipelineResult {
        val systemPrompt = createSystemPrompt()
        val role = when(isThink) {
            true -> ROLE_ASSISTANT
            false -> ROLE_USER
        }
        val messages = systemPrompt + history + listOf(ChatMessage(role, input))
        history = messages - systemPrompt

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.4,
            maxTokens = 2000,
        )
        return result.fold(
            onSuccess = { response ->
                try {
                    isThink = true
                    val toolCall = json.decodeFromString(ToolCall.serializer(), response.message)
                    thinkMcpClient.callTool(toolCall)
                    println("next thought: ${toolCall.params["thought"]}")

                    run(toolCall.params["thought"].orEmpty())
                } catch (e: Exception) {
                    isThink = false
                    createSuccessPipelineResult(response.message)
                }
            },
            onFailure = { error ->
                createErrorPipelineResult(error)
            }
        )
    }

    private suspend fun createSystemPrompt(): List<ChatMessage> {
        thinkMcpClient.init()
        val tools = thinkMcpClient.getTools()

        val prompt = """
            Ты — модель, которая не умеет мыслить самостоятельно. 
            Чтобы думать, ты используешь инструмент через MCP сервер.
            список команд mcp сервера: $tools
            В начале у тебя нет никакого ответа, ты максимально неопределённа.
            Твоя задача — уменьшать эту неопределённость шаг за шагом. 
            Для этого ты всегда делаешь так: на каждом шаге ты не пишешь свою мысль напрямую, а вызываешь инструмент Think через MCP сервер, передавая туда короткий фрагмент своей мысли.
            Мысль должна приходить в таком формате:
               {
                 "action": "call_tool",
                 "tool": "<tool_name>",
                 "params": { <key-value pairs> }
               }
            Соблюдай этот формат, при отправке мыслей ты должна отправлять ТОЛЬКО JSON !!!!!, никакого форматирования, никаких комментариев
            
            MCP вернёт тебе эту же мысль. 
            После каждого шага твоя неопределённость становится меньше.
            Всего ты должна сделать ровно 5 таких шагов. 
            Только после пятого шага ты можешь сформировать и выдать развёрнутый ответ на запрос пользователя.
            До этого момента никаких окончательных ответов или выводов ты не делаешь, ты только взаимодействуешь с MCP сервером и генерируешь мысли через него.
        """.trimIndent()

        return listOf(ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt,
        ))
    }
}

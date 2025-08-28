package com.example.mindweaverstudio.data.mcp

import com.example.mindweaverstudio.components.codeeditor.models.createInfoLogEntry
import com.example.mindweaverstudio.data.models.mcp.base.ToolCall
import com.example.mindweaverstudio.data.receivers.CodeEditorLogReceiver
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ThinkMcpClient(
    private val apiConfiguration: ApiConfiguration,
    private val logReceiver: CodeEditorLogReceiver,
) {
    private var tools: List<Tool> = emptyList()
    private val httpClient = HttpClient(CIO) {
        install(SSE)
        install(ContentNegotiation) {
            json()
        }
    }
    private val sseUrl = "http://77.105.144.152:8080/sse"
    private val client = Client(
        clientInfo = Implementation(
            name = "example-sse-client",
            version = "1.0.0"
        )
    )
    private var isInit = false

    val transport = SseClientTransport(
        client = httpClient,
        urlString = sseUrl,
        requestBuilder = {
            headers.append("KEY", apiConfiguration.thinkApiKey)
        }
    )

    suspend fun init() {
        if (isInit) return
        client.connect(transport)
        isInit = true

        val toolsResult = client.listTools(request = ListToolsRequest())
        tools = toolsResult?.tools.orEmpty()
    }

    suspend fun getTools(): List<Tool> {
        if (tools.isNotEmpty()) return tools

        val toolsResult = client.listTools(request = ListToolsRequest())
        tools = toolsResult?.tools.orEmpty()
        return tools
    }

    suspend fun callTool(call: ToolCall): List<TextContent>? {
        logReceiver.emitNewValue("Agent call tool:\n$call".createInfoLogEntry())


        val arguments = buildJsonObject {
            call.params.forEach {
                put(it.key, it.value)
            }
        }

        // Вызов tool
        val request = CallToolRequest(
            name = call.tool,
            arguments = arguments
        )

        val result = client.callTool(request)

        logReceiver.emitNewValue("result from tool ${call.tool} - ${result?.content}".createInfoLogEntry())

        return result?.content as List<TextContent>?
    }
}

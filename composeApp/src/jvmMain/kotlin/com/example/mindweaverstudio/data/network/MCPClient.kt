package com.example.mindweaverstudio.data.network

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.PipedInputStream
import java.io.PipedOutputStream


class MCPClient() {
    private val clientOut = PipedOutputStream()
    private val serverIn = PipedInputStream(clientOut)
    private val serverOut = PipedOutputStream()
    private val clientIn = PipedInputStream(serverOut)
    private val clientTransport = StdioClientTransport(
        input = clientIn.asSource().buffered(),
        output = clientOut.asSink().buffered()
    )
    private val serverTransport = StdioServerTransport(
        inputStream = serverIn.asSource().buffered(),
        outputStream = serverOut.asSink().buffered()
    )
    private val server = Server(
        serverInfo = Implementation(
            name = "example-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                resources = ServerCapabilities.Resources(
                    subscribe = true,
                    listChanged = true
                )
            )
        )
    )
    val client = Client(
        clientInfo = Implementation(
            name = "example-client",
            version = "1.0.0"
        )
    )

    init {
        server.addResource(
            uri = "file:///hello.txt",
            name = "Hello Resource",
            description = "Just a test file",
            mimeType = "text/plain"
        ) { request ->
            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(
                        text = "Привет, мир из MCP!",
                        uri = request.uri,
                        mimeType = "text/plain"
                    )
                )
            )
        }
    }

    suspend fun getFileFromMcp(): ReadResourceResult? {
        server.connect(serverTransport)
        client.connect(clientTransport)

        val resources = client.listResources()
        println("Resources: ${resources?.resources}")

        return client.readResource(ReadResourceRequest("file:///hello.txt"))
    }
}
package org.example

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            }
        )
    }

    install(Logging) {
        level = LogLevel.INFO
    }

    defaultRequest {
        url {
            protocol = URLProtocol.HTTPS
            host = "api.github.com"
        }
        header("Accept", "application/vnd.github+json")

        val token = System.getenv("GITHUB_TOKEN")
        if (!token.isNullOrEmpty()) {
            header("Authorization", "Bearer $token")
        }
    }
}


fun main() {
    val server = Server(
        serverInfo = Implementation(
            name = "example-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                resources = ServerCapabilities.Resources(
                    subscribe = true,
                    listChanged = true
                ),
                tools = ServerCapabilities.Tools(true),
            )
        )
    )

    server.addTool(
        name = "summarize_commits",
        description = "Возвращает общее количество коммитов и авторов этих коммитов",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("repo", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("owner/repo"))
                })
                put("since", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Дата начала периода (ISO8601), например 2025-08-01T00:00:00Z"))
                })
            },
            required = listOf("repo")
        ),
        outputSchema = Tool.Output(
            properties = buildJsonObject {
                put("total_commits", buildJsonObject { put("type", JsonPrimitive("integer")) })
                put("authors", buildJsonObject { put("type", JsonPrimitive("object")) })
            }
        ),
        toolAnnotations = ToolAnnotations(
            title = "GitHub Commits Summarizer",
            readOnlyHint = true
        )
    ) { req ->
        val repo = req.arguments["repo"]?.jsonPrimitive?.content ?: return@addTool CallToolResult(
            content = listOf(TextContent("Не передан repo")),
            isError = true
        )
        val since = req.arguments["since"]?.jsonPrimitive?.content

        val commitsJson = fetchCommits(repo, since)

        val total = commitsJson.size
        val authors = mutableMapOf<String, Int>()

        for (commit in commitsJson) {
            val author = commit.jsonObject["commit"]?.jsonObject
                ?.get("author")?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "unknown"
            authors[author] = authors.getOrDefault(author, 0) + 1
        }

        val statsJson = buildJsonObject {
            put("total_commits", JsonPrimitive(total))
            put("authors", buildJsonObject {
                authors.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
            })
        }

        CallToolResult(
            content = listOf(TextContent("Коммиты успешно проанализированы")),
            structuredContent = statsJson
        )
    }

    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )

    val scope = CoroutineScope(SupervisorJob())
        scope.launch {
            server.connect(transport)
        }
}

suspend fun fetchCommits(repo: String, since: String? = null): JsonArray {
    val url = buildString {
        append("/repos/$repo/commits")
        if (since != null) append("?since=$since")
    }

    val response: HttpResponse = httpClient.get(url)
    val body = response.bodyAsText()

    return Json.parseToJsonElement(body).jsonArray
}
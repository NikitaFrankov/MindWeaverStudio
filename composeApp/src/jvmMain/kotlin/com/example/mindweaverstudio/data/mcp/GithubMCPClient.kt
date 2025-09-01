package com.example.mindweaverstudio.data.mcp

import com.example.mindweaverstudio.components.codeeditor.models.createInfoLogEntry
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import com.example.mindweaverstudio.data.models.mcp.github.Commit
import com.example.mindweaverstudio.data.models.mcp.base.ToolCall
import com.example.mindweaverstudio.data.models.mcp.github.CreateReleaseResult
import com.example.mindweaverstudio.data.receivers.CodeEditorLogReceiver
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.PipedInputStream
import java.io.PipedOutputStream

class GithubMCPClient(
    private val logReceiver: CodeEditorLogReceiver,
    private val apiConfig: ApiConfiguration,
) {

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
    val httpClient = createHttpClient()

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
                ),
                tools = ServerCapabilities.Tools(true),
            )
        )
    )
    private val client = Client(
        clientInfo = Implementation(
            name = "example-client",
            version = "1.0.0"
        )
    )
    private var tools: List<Tool> = emptyList()

    init {
        initTools()
    }

    suspend fun init() {
        server.connect(serverTransport)
        client.connect(clientTransport)

        val toolsResult = client.listTools(request = ListToolsRequest())
        tools = toolsResult?.tools.orEmpty()
    }

    suspend fun release(): List<TextContent>? {
        // if server is active
        val owner = "NikitaFrankov"
        val repo = "MindWeaverStudio"
        val version = "v0.0.1"
        val changelog = "Simple changelog"

        val arguments = buildJsonObject {
            put("owner", owner)
            put("repo", repo)
            put("version", version)
            put("changelog", changelog)
        }

        val request = CallToolRequest(
            name = "create_release",
            arguments = arguments
        )

        val result = client.callTool(request)

        return result?.content as List<TextContent>?
    }

    suspend fun generateReleaseInfo(): List<TextContent>? {
        val request = CallToolRequest(
            name = "generate_release_info",
        )

        val result = client.callTool(request)

        return result?.content as List<TextContent>?
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

        val request = CallToolRequest(
            name = call.tool,
            arguments = arguments
        )

        val result = client.callTool(request)

        logReceiver.emitNewValue("result from tool ${call.tool} - ${result?.content}".createInfoLogEntry())

        return result?.content as List<TextContent>?
    }

    private fun createHttpClient() = HttpClient(CIO) {
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

            val token = apiConfig.githubApiKey
            if (token.isNotEmpty()) {
                header("Authorization", "Bearer $token")
            }
            header("Accept", "application/vnd.github+json")
        }
    }

    private fun initTools() {
        server.addTool(
            name = "get_commits",
            description = "Get list of commits for a GitHub repository. Input is owner (username or organization) and repo name (e.g. octocat, Hello-World)",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("owner") {
                        put("type", "string")
                        put("description", "GitHub username or organization (e.g. octocat)")
                    }
                    putJsonObject("repo") {
                        put("type", "string")
                        put("description", "Repository name (e.g. Hello-World)")
                    }
                },
                required = listOf("owner", "repo")
            )
        ) { request ->
            val owner = request.arguments["owner"]?.jsonPrimitive?.content ?: return@addTool CallToolResult(
                content = listOf(TextContent("The 'owner' parameter is required."))
            )
            val repo = request.arguments["repo"]?.jsonPrimitive?.content ?: return@addTool CallToolResult(
                content = listOf(TextContent("The 'repo' parameter is required."))
            )
            val commits = httpClient.getCommits(owner, repo)
            CallToolResult(content = commits.map { TextContent(it) })
        }

        server.addTool(
            name = "generate_release_info",
            description = "Generate next release version and changelog based on commits since last release.",
            inputSchema = Tool.Input()
        ) { request ->
            val owner = "NikitaFrankov" // Добавить потом извлечение из конфигурационного файла
            val repo = "MindWeaverStudio" // Добавить потом извлечение из конфигурационного файла

            try {
                val (version, changelog) = generateReleaseInfo(owner, repo, apiConfig.githubApiKey)

                CallToolResult(
                    content = listOf(
                        TextContent("version: $version,\nchangelog:$changelog"),
                    )
                )
            } catch (e: Exception) {
                CallToolResult(content = listOf(TextContent("Failed to generate release info: ${e.message}")))
            }
        }

        server.addTool(
            name = "create_release",
            description = "Create a new GitHub release. Input includes version (tag), and  changelog.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("version") {
                        put("type", "string")
                        put("description", "Release version or tag (e.g. v1.2.0)")
                    }
                    putJsonObject("changelog") {
                        put("type", "string")
                        put("description", "Release description or changelog (Markdown supported)")
                    }
                },
                required = listOf("version", "changelog")
            )
        ) { request ->
            try {
                val version = request.arguments["version"]?.jsonPrimitive?.content
                    ?: return@addTool CallToolResult(
                        content = listOf(TextContent("The 'version' parameter is required."))
                    )
                val changelog = request.arguments["changelog"]?.jsonPrimitive?.content
                    ?: return@addTool CallToolResult(
                        content = listOf(TextContent("The 'changelog' parameter is required."))
                    )

                val result = triggerReleaseWorkflow(
                    owner = "NikitaFrankov",
                    repo = "MindWeaverStudio",
                    version = version,
                    changelog = changelog,
                    token = apiConfig.githubApiKey,
                    client = httpClient
                )

                if (result.success) {
                    CallToolResult(
                        content = listOf(TextContent("Release $version created successfully! URL: ${result.url}"))
                    )
                } else {
                    CallToolResult(
                        content = listOf(TextContent("Failed to create release: ${result.errorMessage}"))
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CallToolResult(content = listOf(TextContent("Exception in create_release: ${e.message}")))
            }
        }
    }

    suspend fun generateReleaseInfo(
        owner: String,
        repo: String,
        token: String
    ): Pair<String, String> { // (newVersion, changelog)
        // 1. Узнаем последний релиз
        val lastReleaseResp = httpClient.get("https://api.github.com/repos/$owner/$repo/releases/latest") {
            header("Authorization", "Bearer $token")
        }

        val lastTag = if (lastReleaseResp.status.isSuccess()) {
            val json = Json.parseToJsonElement(lastReleaseResp.bodyAsText()).jsonObject
            json["tag_name"]?.jsonPrimitive?.content ?: "v0.0.0"
        } else {
            "v0.0.0"
        }

        // 2. Сравнение изменений с main
        val commitMessages = getCommitsSinceLastRelease(
            owner = owner,
            repo = repo,
            token = apiConfig.githubApiKey,
        )

        // 3. Генерация версии (patch bump)
        val versionParts = lastTag.removePrefix("v").split(".").map { it.toInt() }.toMutableList()
        versionParts[2] += 1 // patch bump
        val newVersion = "v${versionParts.joinToString(".")}"

        // 4. Генерация changelog
        val changelog = commitMessages.joinToString("\n") { "- $it" }

        return newVersion to changelog
    }

    suspend fun getCommitsSinceLastRelease(
        owner: String,
        repo: String,
        token: String
    ): List<String> {
        // 1. Пытаемся найти последний релиз
        val lastReleaseResp = httpClient.get("https://api.github.com/repos/$owner/$repo/releases/latest") {
            header("Authorization", "Bearer $token")
        }

        return if (lastReleaseResp.status.isSuccess()) {
            // Есть релиз → берём diff по тегу
            val json = Json.parseToJsonElement(lastReleaseResp.bodyAsText()).jsonObject
            val lastTag = json["tag_name"]?.jsonPrimitive?.content ?: "v0.0.0"

            val compareResp = httpClient.get("https://api.github.com/repos/$owner/$repo/compare/$lastTag...main") {
                header("Authorization", "Bearer $token")
            }
            val commitsJson = Json.parseToJsonElement(compareResp.bodyAsText()).jsonObject
            val commits = commitsJson["commits"]?.jsonArray ?: JsonArray(emptyList())

            commits.map {
                it.jsonObject["commit"]!!
                    .jsonObject["message"]!!
                    .jsonPrimitive.content
            }
        } else {
            // Релизов нет → берём ВСЕ коммиты в main
            val commitsResp = httpClient.get("https://api.github.com/repos/$owner/$repo/commits") {
                header("Authorization", "Bearer $token")
            }
            val commitsJson = Json.parseToJsonElement(commitsResp.bodyAsText()).jsonArray
            commitsJson.map {
                it.jsonObject["commit"]!!
                    .jsonObject["message"]!!
                    .jsonPrimitive.content
            }
        }
    }

    private suspend fun triggerReleaseWorkflow(
        owner: String,
        repo: String,
        version: String,
        changelog: String,
        token: String,
        client: HttpClient,
    ): CreateReleaseResult {
        return try {
            val response = client.post("https://api.github.com/repos/$owner/$repo/actions/workflows/release.yml/dispatches") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github+json")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("ref", "main")
                        putJsonObject("inputs") {
                            put("version", version)
                            put("changelog", changelog)
                        }
                    }
                )
            }

            if (response.status.value == 204) {
                CreateReleaseResult(
                    success = true,
                    url = "Release version $version successfully registered, .dmg file is currently being created."
                )
            } else {
                CreateReleaseResult(
                    success = false,
                    errorMessage = "GitHub API returned status ${response.status.value}: ${response.bodyAsText()}"
                )
            }
        } catch (e: Exception) {
            CreateReleaseResult(success = false, errorMessage = e.message)
        }
    }

    private suspend fun HttpClient.getCommits(owner: String, repo: String): List<String> {
        val uri = "/repos/$owner/$repo/commits"
        val commits = get(uri).body<List<Commit>>()
        return commits.map { commit ->
            """
            SHA: ${commit.sha}
            Message: ${commit.commit.message}
            Author: ${commit.commit.author.name} (${commit.commit.author.email})
            Date: ${commit.commit.author.date}
        """.trimIndent()
        }
    }

}

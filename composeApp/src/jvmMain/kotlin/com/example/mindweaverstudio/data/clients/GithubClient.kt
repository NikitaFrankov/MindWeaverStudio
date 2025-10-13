package com.example.mindweaverstudio.data.clients

import com.example.mindweaverstudio.data.models.mcp.github.Commit
import com.example.mindweaverstudio.data.models.mcp.github.CreateReleaseResult
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class GithubClient(
    private val config: ApiConfiguration,
) {

    private val httpClient = createHttpClient()

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
                protocol = URLProtocol.Companion.HTTPS
                host = "api.github.com"
            }

            val token = config.githubApiKey
            if (token.isNotEmpty()) {
                header("Authorization", "Bearer $token")
            }
            header("Accept", "application/vnd.github+json")
        }
    }



    suspend fun getCommits(owner: String, repo: String): List<String> {
        val uri = "/repos/$owner/$repo/commits"
        val commits = httpClient.get(uri).body<List<Commit>>()
        return commits.map { commit ->
            """
            SHA: ${commit.sha}
            Message: ${commit.commit.message}
            Author: ${commit.commit.author.name} (${commit.commit.author.email})
            Date: ${commit.commit.author.date}
        """.trimIndent()
        }
    }

    suspend fun generateReleaseInfo(
        owner: String,
        repo: String,
    ): Pair<String, String> {
        // 1. Узнаем последний релиз
        val lastReleaseResp = httpClient.get("https://api.github.com/repos/$owner/$repo/releases/latest") {
            header("Authorization", "Bearer ${config.githubApiKey}")
        }

        val lastTag = if (lastReleaseResp.status.isSuccess()) {
            val json = Json.Default.parseToJsonElement(lastReleaseResp.bodyAsText()).jsonObject
            json["tag_name"]?.jsonPrimitive?.content ?: "v0.0.0"
        } else {
            "v0.0.0"
        }

        // 2. Сравнение изменений с main
        val commitMessages = getCommitsSinceLastRelease(
            owner = owner,
            repo = repo,
            token = config.githubApiKey,
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
            val json = Json.Default.parseToJsonElement(lastReleaseResp.bodyAsText()).jsonObject
            val lastTag = json["tag_name"]?.jsonPrimitive?.content ?: "v0.0.0"

            val compareResp = httpClient.get("https://api.github.com/repos/$owner/$repo/compare/$lastTag...main") {
                header("Authorization", "Bearer $token")
            }
            val commitsJson = Json.Default.parseToJsonElement(compareResp.bodyAsText()).jsonObject
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
            val commitsJson = Json.Default.parseToJsonElement(commitsResp.bodyAsText()).jsonArray
            commitsJson.map {
                it.jsonObject["commit"]!!
                    .jsonObject["message"]!!
                    .jsonPrimitive.content
            }
        }
    }

    suspend fun triggerReleaseWorkflow(
        owner: String,
        repo: String,
        version: String,
        changelog: String,
    ): CreateReleaseResult {
        return try {
            val response = httpClient.post("https://api.github.com/repos/$owner/$repo/actions/workflows/release.yml/dispatches") {
                header("Authorization", "Bearer ${config.githubApiKey}")
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
}
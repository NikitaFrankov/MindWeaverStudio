package com.example.mindweaverstudio.data.ai.aiClients

import com.example.mindweaverstudio.data.models.ai.AiResponse
import com.example.mindweaverstudio.data.models.ai.AiResponse.Companion.createTextResponse
import com.example.mindweaverstudio.data.models.chat.local.LocalChatRequest
import com.example.mindweaverstudio.data.models.chat.local.LocalChatResponse
import com.example.mindweaverstudio.data.models.chat.local.Options
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.json.Json

class LocalDeepSeekApiClient() : AiClient {
    private val baseUrl: String = "http://localhost:11434/api/chat"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@LocalDeepSeekApiClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000 // 2 минуты
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 120_000
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    override suspend fun createChatCompletion(
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
    ): Result<AiResponse> {
        val request = LocalChatRequest(
            model = "qwen2.5-coder:7b",
            messages = messages,
            stream = false,
            options = Options {
                localTemperature = temperature
                numContext = maxTokens
            }
        )

        return try {
            println("Request messages = ${request}")
            val response = client.post(baseUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val responseText = response.bodyAsChannel().toInputStream().readBytes().decodeToString()
            println("responseText = $responseText")
            val jsonChunks = responseText.split(Regex("(?<=})\\s*(?=\\{)"))
            println("chunks = $jsonChunks")

            val sb = StringBuilder()
            for (chunk in jsonChunks) {
                if (chunk.isBlank()) continue
                try {
                    val parsed = json.decodeFromString<LocalChatResponse>(chunk)
                    sb.append(parsed.message?.content ?: "")
                    if (parsed.done == true) break  // остановка на последнем чанке
                } catch (_: Exception) {
                    // игнорируем невалидные чанки
                }
            }

            val fullAnswer = sb.toString().trim()
            Result.success(createTextResponse(fullAnswer))
        } catch (e: Exception) {
            println("DeepSeek API Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
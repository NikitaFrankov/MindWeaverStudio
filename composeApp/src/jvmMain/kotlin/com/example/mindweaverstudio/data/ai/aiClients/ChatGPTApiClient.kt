package com.example.mindweaverstudio.data.ai.aiClients

import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import com.example.mindweaverstudio.data.models.ai.AiResponse
import com.example.mindweaverstudio.data.models.ai.AiResponse.Companion.createTextResponse
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatRequest
import com.example.mindweaverstudio.data.models.chat.ChatResponse
import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ChatGPTApiClient(
    private val configuration: ApiConfiguration
) : AiClient {
    private val baseUrl: String = "https://openrouter.ai/api/v1"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@ChatGPTApiClient.json)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 1500000
            connectTimeoutMillis = 500000
            socketTimeoutMillis = 1000000
        }
    }

    override suspend fun createChatCompletion(
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
    ): Result<AiResponse> {
        val request = ChatRequest(
            model = "qwen/qwen2.5-vl-32b-instruct:free",
            messages = messages,
            temperature = temperature,
            maxTokens = 3000,
        )

        return try {
            val response = client.post("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${configuration.openAiApiKey}")
                    append("Content-Type", "application/json")
                }
                setBody(request)
            }
            
            val rawResponse = response.bodyAsText()
            val jsonResponse = json.decodeFromString<ChatResponse>(rawResponse)
            val error = jsonResponse.error
            if (error != null) {
                println("Error in ai response ${error.message + "\n\nError code: ${error.code}"}")
                return Result.failure(Exception(error.message + "\n\nError code: ${error.code}"))
            }
            val content = jsonResponse.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("No response content available"))

            println("Api response $content")
            Result.success(createTextResponse(content))
        } catch (e: Exception) {
            println("ChatGPT API Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}
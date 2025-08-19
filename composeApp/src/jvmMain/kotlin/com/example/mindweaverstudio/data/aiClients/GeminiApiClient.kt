package com.example.mindweaverstudio.data.aiClients

import com.example.mindweaverstudio.data.config.ApiConfiguration
import com.example.mindweaverstudio.data.models.ai.AiResponse
import com.example.mindweaverstudio.data.models.ai.AiResponse.Companion.createTextResponse
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatRequest
import com.example.mindweaverstudio.data.models.chat.ChatResponse
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class GeminiApiClient(
    private val configuration: ApiConfiguration,
) : AiClient {

    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta/openai"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@GeminiApiClient.json)
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
        val request = ChatRequest(
            model = "gemini-2.5-flash",
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
        )

        return try {
            val response = client.post("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${configuration.geminiApiKey}")
                    append("Content-Type", "application/json")
                }
                setBody(request)
            }
            
            val rawResponse = response.bodyAsText()
            val jsonResponse = json.decodeFromString<ChatResponse>(rawResponse)
            val content = jsonResponse.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("No response content available"))

            Result.success(createTextResponse(content))
        } catch (e: Exception) {
            println("Gemini API Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}
package com.example.mindweaverstudio.data.network

import com.example.mindweaverstudio.data.model.chat.ChatRequest
import com.example.mindweaverstudio.data.model.chat.ChatResponse
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class DeepSeekApiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://openrouter.ai/api/v1"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@DeepSeekApiClient.json)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    suspend fun createChatCompletion(request: ChatRequest): Result<ChatResponse> {
        return try {
            val response = client.post("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append("HTTP-Referer", "http://localhost")
                    append("X-Title", "MyApp")
                }
                setBody(request)
            }
            
            // Debug: log raw response
            val rawResponse = response.bodyAsText()
            println("DeepSeek API Response: $rawResponse")
            
            // Try to parse as JSON manually to understand structure
            val jsonResponse = json.decodeFromString<ChatResponse>(rawResponse)
            
            Result.success(jsonResponse)
        } catch (e: Exception) {
            println("DeepSeek API Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}
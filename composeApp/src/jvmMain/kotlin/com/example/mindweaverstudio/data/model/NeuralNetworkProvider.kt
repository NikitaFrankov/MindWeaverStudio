package com.example.mindweaverstudio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NeuralNetworkProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val models: List<String>
) {
    companion object {
        val DEEPSEEK = NeuralNetworkProvider(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            models = listOf("deepseek-chat", "deepseek-coder")
        )
        
        val CHATGPT = NeuralNetworkProvider(
            id = "chatgpt",
            name = "ChatGPT",
            baseUrl = "https://api.openai.com/v1",
            models = listOf("gpt-3.5-turbo", "gpt-4")
        )
        
        val ALL = listOf(DEEPSEEK, CHATGPT)
    }
}
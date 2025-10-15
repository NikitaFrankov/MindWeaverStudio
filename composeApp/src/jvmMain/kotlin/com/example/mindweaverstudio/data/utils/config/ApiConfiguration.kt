package com.example.mindweaverstudio.data.utils.config

import java.util.Properties

data class ApiConfiguration(
    val deepSeekApiKey: String,
    val openRouterKey: String,
    val openAiApiKey: String,
    val geminiApiKey: String,
    val githubApiKey: String,
    val thinkApiKey: String,
) {
    companion object {
        fun load(): ApiConfiguration {
            val properties = Properties()
            
            // Try to load from config file first
            try {
                val configStream = ApiConfiguration::class.java.classLoader
                    .getResourceAsStream("api-config.properties")
                if (configStream != null) {
                    properties.load(configStream)
                }
            } catch (e: Exception) {
                // Config file not found, will use environment variables
            }
            
            return ApiConfiguration(
                deepSeekApiKey = properties.getProperty("deepseek.api.key") 
                    ?: System.getenv("DEEPSEEK_API_KEY") 
                    ?: "",
                openAiApiKey = properties.getProperty("openai.api.key") 
                    ?: System.getenv("OPENAI_API_KEY") 
                    ?: "",
                geminiApiKey = properties.getProperty("gemini.api.key")
                    ?: System.getenv("GEMINI_API_KEY") 
                    ?: "",
                githubApiKey = properties.getProperty("github.api.key")
                    ?: System.getenv("GITHUB_API_KEY")
                    ?: "",
                thinkApiKey = properties.getProperty("think.api.key")
                    ?: System.getenv("think.api.key")
                    ?: "",
                openRouterKey = properties.getProperty("openrouter.api.key")
                    ?: System.getenv("openrouter.api.key")
                    ?: "",
            )
        }
    }
}
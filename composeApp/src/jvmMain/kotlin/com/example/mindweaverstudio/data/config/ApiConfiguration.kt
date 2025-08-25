package com.example.mindweaverstudio.data.config

import java.util.Properties

data class ApiConfiguration(
    val deepSeekApiKey: String,
    val openAiApiKey: String,
    val geminiApiKey: String,
    val githubApiKey: String,
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
            )
        }
    }
}
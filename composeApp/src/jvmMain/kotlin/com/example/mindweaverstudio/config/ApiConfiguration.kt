package com.example.mindweaverstudio.config

data class ApiConfiguration(
    val deepSeekApiKey: String,
    val openAiApiKey: String
) {
    companion object {
        fun load(): ApiConfiguration {
            val properties = java.util.Properties()
            
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
                    ?: "your-deepseek-api-key-here",
                openAiApiKey = properties.getProperty("openai.api.key") 
                    ?: System.getenv("OPENAI_API_KEY") 
                    ?: "your-openai-api-key-here"
            )
        }
    }
}
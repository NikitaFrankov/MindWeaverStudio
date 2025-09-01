package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.ai.aiClients.ChatGPTApiClient
import com.example.mindweaverstudio.data.ai.aiClients.GeminiApiClient
import com.example.mindweaverstudio.data.ai.aiClients.LocalDeepSeekApiClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val aiClientsModule = module {
    single<AiClient>(named("deepseek")) {
        LocalDeepSeekApiClient()
    }
    single<AiClient>(named("gemini")) {
        GeminiApiClient(get())
    }
    single<AiClient>(named("chatgpt")) {
        ChatGPTApiClient(get())
    }
}
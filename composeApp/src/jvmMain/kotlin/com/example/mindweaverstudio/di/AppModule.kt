package com.example.mindweaverstudio.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.components.chat.ChatStoreFactory
import com.example.mindweaverstudio.components.pipeline.PipelineStoreFactory
import com.example.mindweaverstudio.config.ApiConfiguration
import com.example.mindweaverstudio.data.agents.TextReviewerAgent
import com.example.mindweaverstudio.data.agents.TextSummarizerAgent
import com.example.mindweaverstudio.services.SystemPromptService
import com.example.mindweaverstudio.services.DefaultSystemPromptService
import com.example.mindweaverstudio.services.RepositoryProvider
import com.example.mindweaverstudio.services.DefaultRepositoryProvider
import com.example.mindweaverstudio.data.network.ChatGPTApiClient
import com.example.mindweaverstudio.data.network.DeepSeekApiClient
import com.example.mindweaverstudio.data.network.GeminiApiClient
import com.example.mindweaverstudio.data.parsers.StructuredOutputParser
import com.example.mindweaverstudio.data.parsers.ResponseContentParser
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository
import com.example.mindweaverstudio.data.repository.chatgpt.ChatGPTRepositoryImpl
import com.example.mindweaverstudio.data.repository.deepseek.DeepSeekRepositoryImpl
import com.example.mindweaverstudio.data.repository.gemini.GeminiRepositoryImpl
import com.example.mindweaverstudio.data.model.pipeline.Agent
import com.example.mindweaverstudio.data.model.pipeline.AgentPipelineData
import com.example.mindweaverstudio.data.network.MCPClient
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    // Configuration
    single { ApiConfiguration.load() }
    
    // Core
    single<StoreFactory> { DefaultStoreFactory() }
    
    // Network Clients
    single(named("deepseek")) { DeepSeekApiClient(get<ApiConfiguration>().deepSeekApiKey) }
    single(named("chatgpt")) { ChatGPTApiClient(get<ApiConfiguration>().openAiApiKey) }
    single(named("gemini")) { GeminiApiClient(get<ApiConfiguration>().geminiApiKey) }

    // MCP clients
    single { MCPClient() }

    factoryOf(::StructuredOutputParser)
    single { ResponseContentParser(get<StructuredOutputParser>()) }
    
    // Repositories
    single<NeuralNetworkRepository>(named("deepseek")) { DeepSeekRepositoryImpl(get(named("deepseek")), get()) }
    single<NeuralNetworkRepository>(named("chatgpt")) { ChatGPTRepositoryImpl(get(named("chatgpt")), get()) }
    single<NeuralNetworkRepository>(named("gemini")) { GeminiRepositoryImpl(get(named("gemini")), get()) }
    
    // Default repository (can be configured)
    single<NeuralNetworkRepository> { get<NeuralNetworkRepository>(named("chatgpt")) }
    
    // Services
    single<SystemPromptService> { DefaultSystemPromptService() }
    single<RepositoryProvider> { 
        DefaultRepositoryProvider(
            get(named("deepseek")),
            get(named("chatgpt")),
            get(named("gemini"))
        )
    }
    
    // Store Factory
    singleOf(::ChatStoreFactory)

    
    // Pipeline Agents
    factory<Agent<AgentPipelineData, AgentPipelineData>>(named("text_summarizer")) {
        TextSummarizerAgent( get(named("chatgpt")),)
    }

    factory<Agent<AgentPipelineData, AgentPipelineData>>(named("text_reviewer")) {
        TextReviewerAgent( get(named("chatgpt")),)
    }
    
    // Pipeline Store Factory with agents list
    single {
        PipelineStoreFactory(
            get(), 
            listOf(
                get(named("text_summarizer")),
                get(named("text_reviewer")),
            )
        ) 
    }
}
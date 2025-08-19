package com.example.mindweaverstudio.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.components.pipeline.PipelineStoreFactory
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStoreFactory
import com.example.mindweaverstudio.data.config.ApiConfiguration
import com.example.mindweaverstudio.data.agents.TextReviewerAgent
import com.example.mindweaverstudio.data.agents.TextSummarizerAgent
import com.example.mindweaverstudio.data.repository.RepositoryProvider
import com.example.mindweaverstudio.data.repository.DefaultRepositoryProvider
import com.example.mindweaverstudio.data.aiClients.ChatGPTApiClient
import com.example.mindweaverstudio.data.aiClients.DeepSeekApiClient
import com.example.mindweaverstudio.data.aiClients.GeminiApiClient
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository
import com.example.mindweaverstudio.data.repository.chatgpt.ChatGPTRepositoryImpl
import com.example.mindweaverstudio.data.repository.deepseek.DeepSeekRepositoryImpl
import com.example.mindweaverstudio.data.repository.gemini.GeminiRepositoryImpl
import com.example.mindweaverstudio.data.model.pipeline.Agent
import com.example.mindweaverstudio.data.model.pipeline.AgentPipelineData
import com.example.mindweaverstudio.data.mcp.MCPClient
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
    
    // Repositories
    single<NeuralNetworkRepository>(named("deepseek")) { DeepSeekRepositoryImpl(get(named("deepseek"))) }
    single<NeuralNetworkRepository>(named("chatgpt")) { ChatGPTRepositoryImpl(get(named("chatgpt"))) }
    single<NeuralNetworkRepository>(named("gemini")) { GeminiRepositoryImpl(get(named("gemini"))) }
    
    // Default repository (can be configured)
    single<NeuralNetworkRepository> { get<NeuralNetworkRepository>(named("chatgpt")) }
    
    // Services
    single<RepositoryProvider> { 
        DefaultRepositoryProvider(
            get(named("deepseek")),
            get(named("chatgpt")),
            get(named("gemini"))
        )
    }
    
    // Store Factories
    singleOf(::RepositoryManagementStoreFactory)

    
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
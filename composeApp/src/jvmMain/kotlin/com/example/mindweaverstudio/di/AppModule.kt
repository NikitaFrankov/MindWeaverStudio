package com.example.mindweaverstudio.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.components.pipeline.PipelineStoreFactory
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStoreFactory
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStoreFactory
import com.example.mindweaverstudio.data.agents.TextAnalyzerAgentPipeline
import com.example.mindweaverstudio.data.config.ApiConfiguration
import com.example.mindweaverstudio.data.agents.TextReviewerAgent
import com.example.mindweaverstudio.data.agents.TextSummarizerAgent
import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.aiClients.ChatGPTApiClient
import com.example.mindweaverstudio.data.aiClients.DeepSeekApiClient
import com.example.mindweaverstudio.data.aiClients.GeminiApiClient
import com.example.mindweaverstudio.data.models.pipeline.Agent
import com.example.mindweaverstudio.data.models.pipeline.AgentPipelineData
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.data.models.pipeline.TEXT_REVIEWER_AGENT
import com.example.mindweaverstudio.data.models.pipeline.TEXT_SUMMARIZER_AGENT
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    // Configuration
    singleOf(ApiConfiguration::load) bind ApiConfiguration::class

    // Ai Clients
    single<AiClient>(named("deepseek")) {
        DeepSeekApiClient(get())
    }
    single<AiClient>(named("gemini")) {
        GeminiApiClient(get())
    }
    single<AiClient>(named("chatgpt")) {
        ChatGPTApiClient(get())
    }

    // MCP clients
    singleOf(::GithubMCPClient)

    // Pipeline Agents
    factory<Agent<AgentPipelineData, AgentPipelineData>>(named(TEXT_SUMMARIZER_AGENT)) {
        TextSummarizerAgent(get<AiClient>(named("chatgpt")))
    }
    factory<Agent<AgentPipelineData, AgentPipelineData>>(named(TEXT_REVIEWER_AGENT)) {
        TextReviewerAgent(get<AiClient>(named("chatgpt")))
    }
    factory {
        TextAnalyzerAgentPipeline(
            agents = listOf(
                get<Agent<AgentPipelineData, AgentPipelineData>>(named(TEXT_SUMMARIZER_AGENT)),
                get<Agent<AgentPipelineData, AgentPipelineData>>(named(TEXT_REVIEWER_AGENT)),
            )
        )
    }

    // Stores
    singleOf(::DefaultStoreFactory) bind StoreFactory::class
    factoryOf(::PipelineStoreFactory)
    factoryOf(::CodeEditorStoreFactory)
    factory {
        RepositoryManagementStoreFactory(
            githubMcpClient = get(),
            storeFactory = get(),
            aiClient = get<AiClient>(named("chatgpt"))
        )
    }
}
package com.example.mindweaverstudio.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStoreFactory
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionStoreFactory
import com.example.mindweaverstudio.data.ai.orchestrator.CodeOrchestrator
import com.example.mindweaverstudio.data.ai.agents.AgentsOrchestratorFactory
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.mcp.DockerMCPClient
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.data.ai.pipelines.Pipeline
import com.example.mindweaverstudio.data.ai.pipelines.PipelineRegistry
import com.example.mindweaverstudio.data.mcp.ThinkMcpClient
import com.example.mindweaverstudio.data.receivers.CodeEditorLogReceiver
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    // Configuration
    singleOf(ApiConfiguration::load) bind ApiConfiguration::class

    // Receivers
    singleOf(::CodeEditorLogReceiver)

    // Ai Clients
    includes(aiClientsModule)

    // Agents
    includes(agentsModule)

    // Pipelines
    includes(pipelinesModule)

    singleOf(::AgentsOrchestratorFactory)

    //Orchestrator
    factory<CodeOrchestrator> { (pipelineNames: List<String>) ->
        val registry = PipelineRegistry().apply {
            pipelineNames.forEach { pipelineName ->
                register(pipelineName, get<Pipeline>(named(pipelineName)))
            }
        }
        CodeOrchestrator(
            registry = registry,
            aiClient = get<AiClient>(named("chatgpt"))
        )
    }

    // MCP clients
    singleOf(::GithubMCPClient)
    singleOf(::DockerMCPClient)
    singleOf(::ThinkMcpClient)

    // Stores
    singleOf(::DefaultStoreFactory) bind StoreFactory::class
    factoryOf(::ProjectSelectionStoreFactory)
    factoryOf(::CodeEditorStoreFactory)
}
package com.example.mindweaverstudio.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStoreFactory
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionStoreFactory
import com.example.mindweaverstudio.data.agents.ChatAgent
import com.example.mindweaverstudio.data.agents.CodeFixerAgent
import com.example.mindweaverstudio.data.agents.CodeTesterAgent
import com.example.mindweaverstudio.data.agents.TestRunnerAgent
import com.example.mindweaverstudio.data.agents.orchestrator.AgentsOrchestrator
import com.example.mindweaverstudio.data.agents.orchestrator.AgentsOrchestratorFactory
import com.example.mindweaverstudio.data.agents.orchestrator.AgentsRegistry
import com.example.mindweaverstudio.data.config.ApiConfiguration
import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.aiClients.ChatGPTApiClient
import com.example.mindweaverstudio.data.aiClients.DeepSeekApiClient
import com.example.mindweaverstudio.data.aiClients.GeminiApiClient
import com.example.mindweaverstudio.data.mcp.DockerMCPClient
import com.example.mindweaverstudio.data.models.agents.Agent
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.data.models.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.models.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.models.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.models.agents.TEST_RUNNER_AGENT
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
    single<AiClient>(named("deepseek")) {
        DeepSeekApiClient(get())
    }
    single<AiClient>(named("gemini")) {
        GeminiApiClient(get())
    }
    single<AiClient>(named("chatgpt")) {
        ChatGPTApiClient(get())
    }

    // Agents
    factory<Agent>(qualifier = named(CODE_TESTER_AGENT)) {
        CodeTesterAgent(
            aiClient = get<AiClient>(named("chatgpt")),
            dockerMCPClient = get(),
        )
    }
    factory<Agent>(qualifier = named(TEST_RUNNER_AGENT)) {
        TestRunnerAgent(
            aiClient = get<AiClient>(named("chatgpt")),
            dockerMCPClient = get(),
        )
    }
    factory<Agent>(qualifier = named(CHAT_AGENT)) {
        ChatAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
    factory<Agent>(qualifier = named(CODE_FIXER_AGENT)) {
        CodeFixerAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }

    //Orchestrator
    singleOf(::AgentsOrchestratorFactory)
    factory<AgentsOrchestrator> { (agentNames: List<String>) ->
        val registry = AgentsRegistry().apply {
            agentNames.forEach { agentName ->
                register(agentName, get<Agent>(named(agentName)))
            }
        }
        AgentsOrchestrator(
            registry = registry,
            aiClient = get<AiClient>(named("chatgpt"))
        )
    }

    // MCP clients
    singleOf(::GithubMCPClient)
    singleOf(::DockerMCPClient)

    // Stores
    singleOf(::DefaultStoreFactory) bind StoreFactory::class
    factoryOf(::ProjectSelectionStoreFactory)
    factoryOf(::CodeEditorStoreFactory)
}
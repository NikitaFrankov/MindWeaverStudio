package com.example.mindweaverstudio.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStoreFactory
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionStoreFactory
import com.example.mindweaverstudio.data.ai.agents.workers.ChatAgent
import com.example.mindweaverstudio.data.ai.agents.workers.CodeFixerAgent
import com.example.mindweaverstudio.data.ai.agents.workers.TestCreatorAgent
import com.example.mindweaverstudio.data.ai.agents.workers.TestRunnerAgent
import com.example.mindweaverstudio.data.ai.orchestrator.CodeOrchestrator
import com.example.mindweaverstudio.data.ai.agents.AgentsOrchestratorFactory
import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.ai.aiClients.ChatGPTApiClient
import com.example.mindweaverstudio.data.ai.aiClients.DeepSeekApiClient
import com.example.mindweaverstudio.data.ai.aiClients.GeminiApiClient
import com.example.mindweaverstudio.data.mcp.DockerMCPClient
import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.mcp.GithubMCPClient
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.ai.agents.TEST_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.TEST_RUNNER_AGENT
import com.example.mindweaverstudio.data.ai.agents.workers.CodeTesterAgent
import com.example.mindweaverstudio.data.ai.pipelines.CHAT_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.CODE_FIX_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.Pipeline
import com.example.mindweaverstudio.data.ai.pipelines.PipelineFactory
import com.example.mindweaverstudio.data.ai.pipelines.PipelineRegistry
import com.example.mindweaverstudio.data.ai.pipelines.flows.ChatPipeline
import com.example.mindweaverstudio.data.ai.pipelines.flows.CodeFixPipeline
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
    factory<Agent>(qualifier = named(TEST_CREATOR_AGENT)) {
        TestCreatorAgent(
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
            thinkMcpClient = get(),
        )
    }
    factory<Agent>(qualifier = named(CODE_FIXER_AGENT)) {
        CodeFixerAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
    factory<Agent>(qualifier = named(CODE_TESTER_AGENT)) {
        CodeTesterAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }

    singleOf(::PipelineFactory)

    factory<Pipeline>(named(CHAT_PIPELINE)) {
        val agentNames = get<PipelineFactory>().chatPipelineAgents
        val registry = AgentsRegistry().apply {
            agentNames.forEach { agentName ->
                register(agentName, get<Agent>(named(agentName)))
            }
        }
        ChatPipeline(agentsRegistry = registry)
    }

    factory<Pipeline>(named(CODE_FIX_PIPELINE)) {
        val agentNames = get<PipelineFactory>().codeFixerPipelineAgents
        val registry = AgentsRegistry().apply {
            agentNames.forEach { agentName ->
                register(agentName, get<Agent>(named(agentName)))
            }
        }
        CodeFixPipeline(agentsRegistry = registry)
    }

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
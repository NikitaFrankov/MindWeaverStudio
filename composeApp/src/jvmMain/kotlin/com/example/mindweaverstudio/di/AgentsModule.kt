package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.ai.agents.GITHUB_RELEASE_AGENT
import com.example.mindweaverstudio.data.ai.agents.REASONING_AGENT
import com.example.mindweaverstudio.data.ai.agents.RELEASE_NOTES_GENERATION_AGENT
import com.example.mindweaverstudio.data.ai.agents.TEST_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.TEST_RUNNER_AGENT
import com.example.mindweaverstudio.data.ai.agents.workers.ChatAgent
import com.example.mindweaverstudio.data.ai.agents.workers.CodeCreatorAgent
import com.example.mindweaverstudio.data.ai.agents.workers.CodeFixerAgent
import com.example.mindweaverstudio.data.ai.agents.workers.CodeTesterAgent
import com.example.mindweaverstudio.data.ai.agents.workers.GithubReleaseAgent
import com.example.mindweaverstudio.data.ai.agents.workers.ReasoningAgent
import com.example.mindweaverstudio.data.ai.agents.workers.ReleaseNotesGeneratorAgent
import com.example.mindweaverstudio.data.ai.agents.workers.TestCreatorAgent
import com.example.mindweaverstudio.data.ai.agents.workers.TestRunnerAgent
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val agentsModule = module {
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
    factory<Agent>(qualifier = named(CODE_CREATOR_AGENT)) {
        CodeCreatorAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
    factory<Agent>(qualifier = named(RELEASE_NOTES_GENERATION_AGENT)) {
        ReleaseNotesGeneratorAgent(
            aiClient = get<AiClient>(named("chatgpt")),
            mcpClient = get(),
            receiver = get(),
        )
    }
    factory<Agent>(qualifier = named(GITHUB_RELEASE_AGENT)) {
        GithubReleaseAgent(
            aiClient = get<AiClient>(named("chatgpt")),
            mcpClient = get(),
            receiver = get(),
        )
    }
    factory<Agent>(qualifier = named(REASONING_AGENT)) {
        ReasoningAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
}
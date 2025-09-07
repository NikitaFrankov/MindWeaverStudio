package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.data.ai.agents.ARCHITECT_VALIDATOR_OPTIMIZER_AGENT
import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_REVIEWER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.ai.agents.DETAILED_ARCHITECT_DESIGNER_AGENT
import com.example.mindweaverstudio.data.ai.agents.GITHUB_RELEASE_AGENT
import com.example.mindweaverstudio.data.ai.agents.HIGH_LEVEL_ARCHITECT_AGENT
import com.example.mindweaverstudio.data.ai.agents.REASONING_AGENT
import com.example.mindweaverstudio.data.ai.agents.RELEASE_NOTES_GENERATION_AGENT
import com.example.mindweaverstudio.data.ai.agents.REQUIREMENTS_ANALYST_AGENT
import com.example.mindweaverstudio.data.ai.agents.TEST_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.TEST_RUNNER_AGENT
import com.example.mindweaverstudio.data.ai.agents.workers.ChatAgent
import com.example.mindweaverstudio.data.ai.agents.workers.code.CodeCreatorAgent
import com.example.mindweaverstudio.data.ai.agents.workers.code.CodeFixerAgent
import com.example.mindweaverstudio.data.ai.agents.workers.code.CodeReviewerAgent
import com.example.mindweaverstudio.data.ai.agents.workers.code.CodeTesterAgent
import com.example.mindweaverstudio.data.ai.agents.workers.GithubReleaseAgent
import com.example.mindweaverstudio.data.ai.agents.workers.ReasoningAgent
import com.example.mindweaverstudio.data.ai.agents.workers.ReleaseNotesGeneratorAgent
import com.example.mindweaverstudio.data.ai.agents.workers.TestCreatorAgent
import com.example.mindweaverstudio.data.ai.agents.workers.TestRunnerAgent
import com.example.mindweaverstudio.data.ai.agents.workers.architecture.ArchitectValidatorOptimizerAgent
import com.example.mindweaverstudio.data.ai.agents.workers.architecture.DetailedArchitectDesignerAgent
import com.example.mindweaverstudio.data.ai.agents.workers.architecture.HighLevelArchitectAgent
import com.example.mindweaverstudio.data.ai.agents.workers.architecture.RequirementsAnalystAgent
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
    factory<Agent>(qualifier = named(ARCHITECT_VALIDATOR_OPTIMIZER_AGENT)) {
        ArchitectValidatorOptimizerAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
    factory<Agent>(qualifier = named(DETAILED_ARCHITECT_DESIGNER_AGENT)) {
        DetailedArchitectDesignerAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
    factory<Agent>(qualifier = named(HIGH_LEVEL_ARCHITECT_AGENT)) {
        HighLevelArchitectAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
    factory<Agent>(qualifier = named(REQUIREMENTS_ANALYST_AGENT)) {
        RequirementsAnalystAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
    factory<Agent>(qualifier = named(CODE_FIXER_AGENT)) {
        CodeFixerAgent(
            aiClient = get<AiClient>(named("chatgpt")),
        )
    }
    factory<Agent>(qualifier = named(CODE_REVIEWER_AGENT)) {
        CodeReviewerAgent(
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
            authManager = get(),
            limitManager = get(),
        )
    }
}
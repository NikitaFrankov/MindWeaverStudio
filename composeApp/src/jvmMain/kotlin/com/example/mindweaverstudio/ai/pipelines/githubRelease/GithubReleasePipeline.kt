package com.example.mindweaverstudio.ai.pipelines.githubRelease

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.memory.config.MemoryScopeType
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.memory.feature.nodes.nodeLoadFromMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.SingleFact
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.params.LLMParams
import com.example.mindweaverstudio.ai.memory.github.githubAgentScope
import com.example.mindweaverstudio.ai.memory.github.githubOwnerConcept
import com.example.mindweaverstudio.ai.memory.github.githubRepoConcept
import com.example.mindweaverstudio.ai.tools.github.GithubTools
import com.example.mindweaverstudio.ai.memory.project.ProjectContext
import com.example.mindweaverstudio.ai.customStrategy.subgraphs.askMissingFacts.subgraphAskUserMissingFacts
import com.example.mindweaverstudio.ai.memory.DefaultAgentMemoryProvider
import com.example.mindweaverstudio.ai.tools.user.UserInteractionTools
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

const val GITHUB_RELEASE_STRATEGY = "GITHUB_RELEASE_STRATEGY"

class GithubReleasePipeline(
    private val configuration: ApiConfiguration,
    private val githubTools: GithubTools,
    private val userInteractionTools: UserInteractionTools,
    private val memoryProvider: DefaultAgentMemoryProvider,
) {
    private val model = OpenAIModels.CostOptimized.GPT4oMini
    private val requiredConcepts: List<Concept> = listOf(githubOwnerConcept, githubRepoConcept)

    @OptIn(InternalAgentsApi::class)
    private val githubReleaseStrategy = strategy<String, String>(GITHUB_RELEASE_STRATEGY) {
        val subgraphCheckMissingFacts by subgraphAskUserMissingFacts<String>(
            name = "subgraphCheckMissingFacts",
            userConnectionTools = userInteractionTools.asTools(),
            requiredConcepts = requiredConcepts,
            memorySubject = ProjectContext,
            memoryScope = githubAgentScope,
            llmModel = model,
            llmParams = LLMParams().copy(temperature = 0.3)
        )

        val nodeLoadFacts by nodeLoadFromMemory<String>(
            concepts = requiredConcepts,
            scope = MemoryScopeType.AGENT,
            subject = ProjectContext,
            name = "nodeLoadFacts",
        )

        val releaseNotes by subgraphWithTask<String, String>(
            tools = githubTools.asTools() + userInteractionTools.asTools(),
            llmParams = LLMParams().copy(
                temperature = 0.3
            ),
        ) { releaseNotesAgentSystemPrompt }

        val nodeRelease by subgraphWithTask<String, String>(
            tools = githubTools.asTools()+ userInteractionTools.asTools(),
            llmParams = LLMParams().copy(
                temperature = 0.3
            ),
        ) { input -> "Create github release with: $input" }

        edge(nodeStart forwardTo subgraphCheckMissingFacts)
        edge(subgraphCheckMissingFacts forwardTo nodeLoadFacts)
        edge(nodeLoadFacts forwardTo releaseNotes)
        edge(releaseNotes forwardTo nodeRelease)
        edge(nodeRelease forwardTo nodeFinish)
    }

    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(configuration.openAiApiKey),
        strategy = githubReleaseStrategy,
        toolRegistry = ToolRegistry {
            tools(githubTools)
            tools(userInteractionTools)
        },
        llmModel = model,
        temperature = 0.1
    ) {
        install(AgentMemory) {
            memoryProvider = this@GithubReleasePipeline.memoryProvider
            agentName = "github-pipeline-agent"
            featureName = "github-pipeline-feature"
            organizationName = "radionov"
            productName = "mind-weaver-studio"
        }
        install(Tracing) {

        }
    }

    suspend fun run(
        input: String,
        repoName: String,
        repoOwner: String,
    ): String {
        saveGithubInfo(repoOwner, repoName)
        return agent.run(input)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun saveGithubInfo(repoOwner: String, repoName: String) {
        if (repoOwner.isNotEmpty()) {
            val ownerFact = SingleFact(
                concept = githubOwnerConcept,
                value = repoOwner,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            memoryProvider.save(
                fact = ownerFact,
                subject = ProjectContext,
                scope = githubAgentScope
            )
        }
        if (repoName.isNotEmpty()) {
            val repoFact = SingleFact(
                concept = githubRepoConcept,
                value = repoName,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            memoryProvider.save(
                fact = repoFact,
                subject = ProjectContext,
                scope = githubAgentScope
            )
        }
    }
}
package com.example.mindweaverstudio.ai.pipelines.githubRelease

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.memory.feature.nodes.nodeLoadAllFactsFromMemory
import ai.koog.agents.memory.model.DefaultTimeProvider
import ai.koog.agents.memory.model.SingleFact
import ai.koog.agents.memory.providers.LocalFileMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.SimpleStorage
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.params.LLMParams
import ai.koog.rag.base.files.JVMFileSystemProvider
import com.example.mindweaverstudio.ai.memory.github.githubAgentScope
import com.example.mindweaverstudio.ai.memory.github.githubOwnerConcept
import com.example.mindweaverstudio.ai.memory.github.githubRepoConcept
import com.example.mindweaverstudio.ai.tools.github.GithubTools
import com.example.mindweaverstudio.ai.memory.project.ProjectContext
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import kotlin.io.path.Path as JavaPath

const val GITHUB_RELEASE_STRATEGY = "GITHUB_RELEASE_STRATEGY"

class GithubReleasePipeline(
    private val configuration: ApiConfiguration,
    private val tools: GithubTools,
) {
    private val model = OpenAIModels.CostOptimized.GPT4oMini
    private val memoryProvider = LocalFileMemoryProvider(
        config = LocalMemoryConfig("mind-weaver-studio"),
        storage = SimpleStorage(JVMFileSystemProvider.ReadWrite),
        fs = JVMFileSystemProvider.ReadWrite,
        root = JavaPath("")
    )

    private val githubReleaseStrategy = strategy<String, String>(GITHUB_RELEASE_STRATEGY) {
        val nodeLoadFacts by nodeLoadAllFactsFromMemory<String>(name = "nodeLoadFacts")

        val releaseNotes by subgraphWithTask<String, String>(
            tools = tools.asTools(),
            llmParams = LLMParams().copy(
                temperature = 0.3
            ),
        ) { releaseNotesAgentSystemPrompt }

        val nodeRelease by subgraphWithTask<String, String>(
            tools = tools.asTools(),
            llmParams = LLMParams().copy(
                temperature = 0.3
            ),
        ) { input -> "Create github release with: $input" }

        edge(nodeStart forwardTo nodeLoadFacts)
        edge(nodeLoadFacts forwardTo releaseNotes)
        edge(releaseNotes forwardTo nodeRelease)
        edge(nodeRelease forwardTo nodeFinish)
    }

    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(configuration.openAiApiKey),
        strategy = githubReleaseStrategy,
        toolRegistry = ToolRegistry {
            tools(tools)
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

    private suspend fun saveGithubInfo(repoOwner: String, repoName: String) {
        val ownerConcept = githubOwnerConcept
        val repoNameConcept = githubRepoConcept
        val ownerFact = SingleFact(
            concept = ownerConcept,
            value = repoOwner,
            timestamp = DefaultTimeProvider.getCurrentTimestamp()
        )
        val repoFact = SingleFact(
            concept = repoNameConcept,
            value = repoName,
            timestamp = DefaultTimeProvider.getCurrentTimestamp()
        )

        memoryProvider.save(
            fact = ownerFact,
            subject = ProjectContext,
            scope = githubAgentScope
        )
        memoryProvider.save(
            fact = repoFact,
            subject = ProjectContext,
            scope = githubAgentScope
        )
    }
}
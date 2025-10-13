package com.example.mindweaverstudio.data.ai.pipelines.githubRelease

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.DefaultTimeProvider
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.SingleFact
import ai.koog.agents.memory.providers.LocalFileMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.Aes256GCMEncryptor
import ai.koog.agents.memory.storage.EncryptedStorage
import ai.koog.agents.memory.storage.SimpleStorage
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.params.LLMParams
import ai.koog.rag.base.files.JVMFileSystemProvider
import com.example.mindweaverstudio.data.ai.tools.github.GithubTools
import com.example.mindweaverstudio.data.models.ai.memory.ProjectContext
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import kotlin.io.path.Path

const val GITHUB_RELEASE_STRATEGY = "GITHUB_RELEASE_STRATEGY"

class GithubReleasePipeline(
    config: ApiConfiguration,
    private val tools: GithubTools,
) {
    private val memoryScope: MemoryScope = MemoryScope.Agent("GithubReleasePipeline")
//    private val secureStorage = EncryptedStorage(
//        fs = JVMFileSystemProvider.ReadWrite,
//        encryption = Aes256GCMEncryptor("my-secret-key")
//    )
    private val memoryProvider = LocalFileMemoryProvider(
        config = LocalMemoryConfig("mind-weaver-studio"),
        storage = SimpleStorage(JVMFileSystemProvider.ReadWrite),
        fs = JVMFileSystemProvider.ReadWrite,
        root = Path("")
    )

    private val githubReleaseStrategy = strategy<String, String>(GITHUB_RELEASE_STRATEGY) {
        val releaseNotes by subgraphWithTask<String, String>(
            tools = ToolRegistry { tools(tools) }.tools,
            llmModel = OpenAIModels.CostOptimized.O3Mini,
            llmParams = LLMParams().copy(
                temperature = 0.3
            ),
        ) { releaseNotesAgentSystemPrompt }


        val nodeRelease by subgraphWithTask<String, String>(
            tools = ToolRegistry { tools(tools) }.tools,
            llmModel = OpenAIModels.CostOptimized.O3Mini,
            llmParams = LLMParams().copy(
                temperature = 0.3
            ),
        ) { input -> "Create github release with: $input" }

        edge(nodeStart forwardTo releaseNotes)
        edge(releaseNotes forwardTo nodeRelease)
        edge(nodeRelease forwardTo nodeFinish)
    }

    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(config.openAiApiKey),
        strategy = githubReleaseStrategy,
        llmModel = OpenAIModels.CostOptimized.O3Mini,
        temperature = 0.1
    ) {
        install(AgentMemory) {
            memoryProvider = this@GithubReleasePipeline.memoryProvider
            agentName = "github-pipeline-agent"
            featureName = "github-pipeline-feature"
            organizationName = "radionov"
            productName = "mind-weaver-studio"
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
        memoryProvider.save(
            fact = SingleFact(
                value = "$repoOwner/$repoName",
                concept = Concept("github-repo", "Current GitHub repository in format 'owner/name'", FactType.SINGLE),
                timestamp = DefaultTimeProvider.getCurrentTimestamp()
            ),
            subject = ProjectContext,
            scope = memoryScope,
        )
    }
}
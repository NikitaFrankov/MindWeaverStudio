package com.example.mindweaverstudio.data.ai.pipelines.githubRelease

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentSubgraph
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.memory.config.MemoryScopeType
import ai.koog.agents.memory.feature.nodes.nodeSaveToMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.params.LLMParams
import com.example.mindweaverstudio.data.ai.pipelines.architecture.nodeHighLevelSystemPrompt
import com.example.mindweaverstudio.data.ai.tools.github.GithubTools
import com.example.mindweaverstudio.data.settings.Settings
import com.example.mindweaverstudio.data.settings.SettingsKey.*
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import kotlinx.serialization.Serializable

const val GITHUB_RELEASE_STRATEGY = "GITHUB_RELEASE_STRATEGY"

@Serializable
data object GithubInfo : MemorySubject() {
    override val name: String = "github information"
    override val promptDescription: String = "All important information of user github.com"
    override val priorityLevel: Int = 1
}

class GithubReleasePipeline(
    config: ApiConfiguration,
    private val tools: GithubTools,
) {

    private val repoName: String = ""
    private val repoOwner: String = ""

    private val githubReleaseStrategy = strategy<String, String>(GITHUB_RELEASE_STRATEGY) {
        val githubInfo by nodeSaveToMemory<String>(
            name = "githubInfo",
            concepts = listOf(
                Concept(repoName, "Github repository name", FactType.MULTIPLE),
                Concept(repoOwner, "Github repository owner name", FactType.MULTIPLE)
            ),
            subject = GithubInfo,
            scope = MemoryScopeType.AGENT,
        )

        val releaseNotes by subgraphWithTask<String, String>(
            tools = ToolRegistry { tools(tools) }.tools,
            llmModel = OpenAIModels.CostOptimized.O3Mini,
            llmParams = LLMParams().copy(
                temperature = 0.3
            ),
        ) { releaseNotesAgentSystemPrompt }


        // 2. Этап: Высокоуровневая архитектура
        val nodeRelease by node<String, String> { input: String ->
            llm.writeSession {
                model = OpenAIModels.CostOptimized.O3Mini
                updatePrompt {
                    system(nodeHighLevelSystemPrompt)
                    user(input)
                }
                val response = requestLLMWithoutTools()
                response.content



                // try {
                //            val toolCall = json.decodeFromString(ToolCall.serializer(), message)
                //            val result = mcpClient.callTool(toolCall)?.firstOrNull()?.text.orEmpty()
                //            val toolLogEntry = "Tool for creating github release has completed his work. result:\n$result".createInfoLogEntry()
                //
                //            receiver.emitNewValue(toolLogEntry)
                //
                //            return successPipelineResult(result)
                //        } catch (e: Exception) {
                //            return errorPipelineResult(e)
                //        }
            }
        }

        edge(nodeStart forwardTo nodeFinish)
    }

    private val promptExecutor = simpleOpenAIExecutor(config.openAiApiKey)
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = githubReleaseStrategy,
        llmModel = OpenAIModels.CostOptimized.O3Mini,
        temperature = 0.1
    )

    suspend fun run(
        input: String,
        repoName: String,
        repoOwner: String,
    ): String {
        return agent.run(input)
    }
}
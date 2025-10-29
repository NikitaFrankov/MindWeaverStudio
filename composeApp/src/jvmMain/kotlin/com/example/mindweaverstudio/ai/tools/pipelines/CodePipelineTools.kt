package com.example.mindweaverstudio.ai.tools.pipelines

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.example.mindweaverstudio.ai.pipelines.architecture.ArchitecturePipeline
import com.example.mindweaverstudio.ai.pipelines.chat.ChatPipeline
import com.example.mindweaverstudio.ai.pipelines.codeCreator.CodeCreatorPipeline
import com.example.mindweaverstudio.ai.pipelines.codeFix.CodeFixPipeline
import com.example.mindweaverstudio.ai.pipelines.githubRelease.GithubReleasePipeline
import com.example.mindweaverstudio.data.models.repository.RepositoryInfo
import com.example.mindweaverstudio.data.settings.Settings
import com.example.mindweaverstudio.data.settings.SettingsKey.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CodePipelineTools(
    private val githubReleasePipeline: GithubReleasePipeline,
    private val architecturePipeline: ArchitecturePipeline,
    private val codeCreatorPipeline: CodeCreatorPipeline,
    private val codeFixPipeline: CodeFixPipeline,
    private val chatPipeline: ChatPipeline,
    private val settings: Settings,
) : ToolSet {

    private val json = Json {  }

    @Tool
    @LLMDescription("Starts the pipeline for creating the application architecture")
    suspend fun runArchitecturePipeline(
        @LLMDescription("Initial description of the task from the user")
        userRequest: String
    ): String {
        return withContext(Dispatchers.Default) {
            architecturePipeline.run(userRequest)
        }
    }

    @Tool
    @LLMDescription("Starts the pipeline for creating code")
    suspend fun runCodeCreationPipeline(
        @LLMDescription("Initial description of the task from the user")
        userRequest: String
    ): String {
        return withContext(Dispatchers.Default) {
            codeCreatorPipeline.run(userRequest)
        }
    }

    @Tool
    @LLMDescription("Starts the pipeline for default chat with user")
    suspend fun runChatPipeline(
        @LLMDescription("User message")
        userMessage: String
    ): String {
        return withContext(Dispatchers.Default) {
            chatPipeline.run(userMessage)
        }
    }

    @Tool
    @LLMDescription("Starts the pipeline for fix code")
    suspend fun runCodeFixPipeline(
        @LLMDescription("Initial description of the task from the user")
        userRequest: String
    ): String {
        return withContext(Dispatchers.Default) {
            codeFixPipeline.run(userRequest)
        }
    }

    @Tool
    @LLMDescription("Starts the pipeline for create github release")
    suspend fun runGithubReleasePipeline(
        @LLMDescription("Initial description of the task from the user")
        userRequest: String
    ): String {
        return withContext(Dispatchers.Default) {
            val repositoryInfo = getRepositoryInfo()

            githubReleasePipeline.run(
                input = userRequest,
                repoName = repositoryInfo.name,
                repoOwner = repositoryInfo.owner,
            )
        }
    }

    private suspend fun getRepositoryInfo(): RepositoryInfo {
        val currentProjectPath = settings.getString(CurrentProjectPath)
        val reportString = settings.getString(ProjectRepoInformation(currentProjectPath))

        if (reportString.isEmpty()) {
            return RepositoryInfo()
        }

        return json.decodeFromString(
            deserializer = RepositoryInfo.serializer(),
            string = settings.getString(ProjectRepoInformation(currentProjectPath))
        )
    }
}
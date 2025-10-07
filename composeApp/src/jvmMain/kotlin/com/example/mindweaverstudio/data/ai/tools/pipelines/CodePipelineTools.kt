package com.example.mindweaverstudio.data.ai.tools.pipelines

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.example.mindweaverstudio.data.ai.pipelines.architecture.ArchitecturePipeline
import com.example.mindweaverstudio.data.ai.pipelines.chat.ChatPipeline
import com.example.mindweaverstudio.data.ai.pipelines.codeCreator.CodeCreatorPipeline
import com.example.mindweaverstudio.data.ai.pipelines.codeFix.CodeFixPipeline
import com.example.mindweaverstudio.data.ai.pipelines.githubRelease.GithubReleasePipeline
import com.example.mindweaverstudio.data.settings.Settings
import com.example.mindweaverstudio.data.settings.SettingsKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodePipelineTools(
    private val githubReleasePipeline: GithubReleasePipeline,
    private val architecturePipeline: ArchitecturePipeline,
    private val codeCreatorPipeline: CodeCreatorPipeline,
    private val codeFixPipeline: CodeFixPipeline,
    private val chatPipeline: ChatPipeline,

    private val settings: Settings,
) : ToolSet {

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
    suspend fun runGithubRelease(
        @LLMDescription("Initial description of the task from the user")
        userRequest: String
    ): String {
        return withContext(Dispatchers.Default) {
            val repoName = settings.getString(SettingsKey.GITHUB_REPO_NAME)
            val repoOwner = settings.getString(SettingsKey.GITHUB_REPO_OWNER)

            githubReleasePipeline.run(
                input = userRequest,
                repoName = repoName,
                repoOwner = repoOwner,
            )
        }
    }
}
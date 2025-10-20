package com.example.mindweaverstudio.data.ai.tools.github

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.example.mindweaverstudio.data.clients.GithubClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubTools(
    private val githubClient: GithubClient
) : ToolSet {

    @Tool(customName = "generateReleaseInfo")
    @LLMDescription("Generate next release version and changelog based on commits since last release.")
    suspend fun generateReleaseInfo(
        @LLMDescription("GitHub username or organization")
        owner: String,
        @LLMDescription("Repository name")
        repo: String,
    ): String {
        val (version, changelog) = githubClient.generateReleaseInfo(owner, repo)

        return "version: $version,\nchangelog:$changelog"
    }

    @Tool(customName = "createNextGithubRelease")
    @LLMDescription("Create a new GitHub release. Input includes version (tag), and  changelog.")
    suspend fun createNextGithubRelease(
        @LLMDescription("Release version or tag (e.g. v1.2.0)")
        version: String,
        @LLMDescription("Release description or changelog (Markdown supported)")
        changelog: String,
        @LLMDescription("GitHub username or organization")
        owner: String,
        @LLMDescription("Repository name")
        repo: String,
    ): String {
        val result = githubClient.triggerReleaseWorkflow(
            owner = owner,
            repo = repo,
            version = version,
            changelog = changelog,
        )

        return "Release $version created successfully! URL: ${result.url}"
    }

}
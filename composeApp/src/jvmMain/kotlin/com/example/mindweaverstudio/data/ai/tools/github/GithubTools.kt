package com.example.mindweaverstudio.data.ai.tools.github

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.example.mindweaverstudio.data.ai.tools.clients.GithubClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubTools(
    private val githubClient: GithubClient
) : ToolSet {

    @Tool
    @LLMDescription("Get list of commits for a GitHub repository. Input is owner (username or organization) and repo name (e.g. octocat, Hello-World)")
    suspend fun getCommits(
        @LLMDescription("GitHub username or organization")
        owner: String,
        @LLMDescription("Repository name")
        repo: String,
    ): List<String> {
        val commits = withContext(Dispatchers.Default) {
            githubClient.getCommits(owner, repo)
        }

        return commits
    }

    @Tool
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

    @Tool
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
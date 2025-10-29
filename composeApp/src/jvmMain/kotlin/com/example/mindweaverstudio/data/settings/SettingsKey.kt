package com.example.mindweaverstudio.data.settings

sealed class SettingsKey(val value: String) {
    class ProjectRepoInformation(val path: String) : SettingsKey(value = path)

    data object CurrentProjectPath : SettingsKey(value = "current_project_path")
    data object GithubRepoOwner : SettingsKey(value = "github_repo_owner")
    data object GithubRepoName : SettingsKey(value = "github_repo_name")
    data object TokenKey : SettingsKey(value = "auth_token")
}
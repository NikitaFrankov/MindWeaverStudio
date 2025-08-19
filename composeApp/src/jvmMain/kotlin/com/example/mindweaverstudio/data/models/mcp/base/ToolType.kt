package com.example.mindweaverstudio.data.models.mcp.base

enum class ToolType(val value: String) {
    UNKNOWN("unknown"),
    FETCH_COMMITS("fetch_commits"),
    RUN_PROJECT_CONTAINER("run_project_container");

    companion object {
        fun valueeOf(name: String): ToolType = when(name) {
            FETCH_COMMITS.value -> FETCH_COMMITS
            RUN_PROJECT_CONTAINER.value -> RUN_PROJECT_CONTAINER
            else -> UNKNOWN
        }
    }
}
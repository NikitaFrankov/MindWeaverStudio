package com.example.mindweaverstudio.data.models.mcp.github

import kotlinx.serialization.Serializable

@Serializable
data class Commit(
    val sha: String,
    val commit: CommitDetails
)

@Serializable
data class CommitDetails(
    val author: Author,
    val message: String
)

@Serializable
data class Author(
    val name: String,
    val email: String,
    val date: String
)
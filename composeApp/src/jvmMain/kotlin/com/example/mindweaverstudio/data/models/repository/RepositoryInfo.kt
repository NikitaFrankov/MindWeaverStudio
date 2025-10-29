package com.example.mindweaverstudio.data.models.repository

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryInfo(
    val owner: String = "",
    val name: String = "",
)
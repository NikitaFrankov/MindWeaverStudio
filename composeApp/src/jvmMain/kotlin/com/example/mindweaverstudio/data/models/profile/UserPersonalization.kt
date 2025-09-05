package com.example.mindweaverstudio.data.models.profile

import kotlinx.serialization.Serializable

@Serializable
data class UserPersonalization(
    val name: String = "User",
    val habits: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
    val history: List<String> = emptyList()
)
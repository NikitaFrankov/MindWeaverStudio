package com.example.mindweaverstudio.data.models.profile

import kotlinx.serialization.Serializable

@Serializable
data class UserPersonalization(
    val name: String = "User",
    val role: WorkRole = WorkRole.DEVELOPER,
    val preferredLanguage: String = "Kotlin",
    val responseFormat: ResponseFormat = ResponseFormat.MARKDOWN,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val timeZone: String = "UTC",
    val habits: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
    val history: List<String> = emptyList()
)

@Serializable
enum class WorkRole(val displayName: String) {
    DEVELOPER("Developer"),
    MANAGER("Manager"),
    DESIGNER("Designer"),
    QA_ENGINEER("QA Engineer"),
    DEVOPS("DevOps Engineer"),
    ARCHITECT("Architect"),
    STUDENT("Student"),
    OTHER("Other")
}

@Serializable
enum class ResponseFormat(val displayName: String) {
    MARKDOWN("Markdown"),
    PLAIN_TEXT("Plain Text")
}

@Serializable
enum class ExperienceLevel(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    EXPERT("Expert")
}
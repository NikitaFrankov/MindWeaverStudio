package com.example.mindweaverstudio.data.model.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequirementsSummary(
    @SerialName("schema_version") val schemaVersion: String,
    @SerialName("project_name") val projectName: String,
    val category: String,
    val data: ProjectData,
    val summary: String,
    val meta: RequirementsMeta
)

@Serializable
data class ProjectData(
    val goal: String,
    val constraints: List<String>,
    @SerialName("success_criteria") val successCriteria: List<String>,
    val steps: List<String>,
    val timeline: Timeline,
    val budget: Budget,
    val stakeholders: List<Stakeholder>,
    val metrics: List<String>,
    @SerialName("additional_fields") val additionalFields: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()
)

@Serializable
data class Timeline(
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val milestones: List<Milestone> = emptyList()
)

@Serializable
data class Milestone(
    val name: String,
    val date: String? = null
)

@Serializable
data class Budget(
    @SerialName("amount_min") val amountMin: Double? = null,
    @SerialName("amount_max") val amountMax: Double? = null,
    val currency: String? = null
)

@Serializable
data class Stakeholder(
    val role: String,
    val name: String? = null,
    val contact: String? = null
)

@Serializable
data class RequirementsMeta(
    @SerialName("collected_at") val collectedAt: String,
    val complete: Boolean,
    val confidence: Double
)
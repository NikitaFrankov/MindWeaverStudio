package com.example.mindweaverstudio.data.model.chat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class StructuredOutput(
    val formatVersion: String,
    val type: String,
    val answer: Answer,
    val points: List<Point> = emptyList(),
    val summary: Summary,
    val meta: Meta? = null
)

@Serializable
data class Answer(val value: JsonElement, val type: String)

@Serializable
data class Point(val kind: String, val text: String)

@Serializable
data class Summary(val text: String, val length: String)

@Serializable
data class Meta(val confidence: Double? = null, val source: String? = null)
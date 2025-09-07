package com.example.mindweaverstudio.data.utils.ragchunking.models

import kotlinx.serialization.Serializable

@Serializable
data class RAGDocument(
    val id: String,
    val content: String,
    val metadata: Map<String, String>
)
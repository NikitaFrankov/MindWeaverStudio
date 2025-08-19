package com.example.mindweaverstudio.data.models.pipeline

const val TEXT_REVIEWER_AGENT = "Text Reviewer"
const val TEXT_SUMMARIZER_AGENT = "Text Summarizer"

interface Agent<I, O> {
    val name: String
    suspend fun run(input: I): O
}
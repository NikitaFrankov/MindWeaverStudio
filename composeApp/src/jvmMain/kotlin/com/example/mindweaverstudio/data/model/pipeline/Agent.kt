package com.example.mindweaverstudio.data.model.pipeline

interface Agent<I, O> {
    val name: String
    suspend fun run(input: I): O
}
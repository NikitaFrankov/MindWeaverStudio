package com.example.mindweaverstudio.data.ai.agents

import com.example.mindweaverstudio.data.models.pipeline.PipelineResult

const val TEST_CREATOR_AGENT = "Test Creator"
const val CODE_TESTER_AGENT = "Code Tester"
const val CODE_FIXER_AGENT = "Code Fixer"
const val TEST_RUNNER_AGENT = "Test Runner"
const val CHAT_AGENT = "Chat"

interface Agent {
    val name: String
    val description: String
    suspend fun run(input: String): PipelineResult
}


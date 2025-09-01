package com.example.mindweaverstudio.data.ai.agents

import com.example.mindweaverstudio.data.models.pipeline.PipelineResult

const val TEST_CREATOR_AGENT = "Test Creator"
const val CODE_TESTER_AGENT = "Code Tester"
const val CODE_FIXER_AGENT = "Code Fixer"
const val CODE_CREATOR_AGENT = "Code Creator"
const val TEST_RUNNER_AGENT = "Test Runner"
const val RELEASE_NOTES_GENERATION_AGENT = "release_notes_generation_agent"
const val GITHUB_RELEASE_AGENT = "github_release_agent"
const val CHAT_AGENT = "Chat"

interface Agent {
    val name: String
    val description: String
    suspend fun run(input: String): PipelineResult
}


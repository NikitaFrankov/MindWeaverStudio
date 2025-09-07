package com.example.mindweaverstudio.data.ai.agents

import com.example.mindweaverstudio.data.models.pipeline.PipelineResult

const val RELEASE_NOTES_GENERATION_AGENT = "release_notes_generation_agent"
const val GITHUB_RELEASE_AGENT = "github_release_agent"
const val CODE_REVIEWER_AGENT = "code_reviewer_agent"
const val TEST_CREATOR_AGENT = "test_creator_agent"
const val CODE_CREATOR_AGENT = "code_creator_agent"
const val TEST_RUNNER_AGENT = "test_runner_agent"
const val CODE_TESTER_AGENT = "code_tester_agent"
const val CODE_FIXER_AGENT = "code_fixer_agent"
const val REASONING_AGENT = "reasoning_agent"
const val CHAT_AGENT = "chat_agent"

interface Agent {
    val name: String
    val description: String
    suspend fun run(input: String): PipelineResult
}


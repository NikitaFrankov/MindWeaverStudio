package com.example.mindweaverstudio.data.models.agents

import com.example.mindweaverstudio.data.models.chat.ChatMessage

const val CODE_TESTER_AGENT = "Code Tester"
const val CODE_FIXER_AGENT = "Code Tester"
const val TEST_RUNNER_AGENT = "Test Runner"
const val CHAT_AGENT = "Chat"

interface Agent {
    val name: String
    val description: String
    suspend fun run(input: ChatMessage): AgentResult
}


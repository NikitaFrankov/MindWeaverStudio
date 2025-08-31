package com.example.mindweaverstudio.data.ai.pipelines

import com.example.mindweaverstudio.data.ai.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_CREATOR_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT

class PipelineFactory {
    val codeFixerPipelineAgents = listOf(CODE_FIXER_AGENT, CODE_TESTER_AGENT)
    val chatPipelineAgents = listOf(CHAT_AGENT)
    val codeCreatorPipelineAgents = listOf(CODE_CREATOR_AGENT)
}
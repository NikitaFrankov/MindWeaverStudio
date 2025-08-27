package com.example.mindweaverstudio.data.agents.orchestrator

import com.example.mindweaverstudio.data.models.agents.CHAT_AGENT
import com.example.mindweaverstudio.data.models.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.models.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.models.agents.TEST_RUNNER_AGENT
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

class AgentsOrchestratorFactory {
    val editorOrchestrator: AgentsOrchestrator by inject(AgentsOrchestrator::class.java) {
        parametersOf(listOf(CHAT_AGENT, TEST_RUNNER_AGENT, CODE_TESTER_AGENT, CODE_FIXER_AGENT))
    }
}
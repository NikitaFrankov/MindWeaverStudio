package com.example.mindweaverstudio.data.ai.agents

import com.example.mindweaverstudio.data.ai.orchestrator.CodeOrchestrator
import com.example.mindweaverstudio.data.ai.pipelines.ARCHITECTURE_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.CHAT_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.CODE_CREATOR_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.CODE_FIX_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.CODE_REVIEW_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.GITHUB_RELEASE_PIPELINE
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent

class AgentsOrchestratorFactory {
    val editorOrchestrator: CodeOrchestrator by KoinJavaComponent.inject(CodeOrchestrator::class.java) {
        parametersOf(
            listOf(
                CODE_FIX_PIPELINE,
                CHAT_PIPELINE,
                CODE_CREATOR_PIPELINE,
                GITHUB_RELEASE_PIPELINE,
                CODE_REVIEW_PIPELINE,
                ARCHITECTURE_PIPELINE
            )
        )
    }
}
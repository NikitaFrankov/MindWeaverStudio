package com.example.mindweaverstudio.data.ai.pipelines

import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.ai.pipelines.flows.CodeFixPipeline
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent

class PipelineFactory {
    val codeFixerPipelineAgents = listOf(CODE_FIXER_AGENT, CODE_TESTER_AGENT)
}
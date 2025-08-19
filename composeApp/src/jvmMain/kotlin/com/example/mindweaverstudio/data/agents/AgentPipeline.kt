package com.example.mindweaverstudio.data.agents

import com.example.mindweaverstudio.data.model.pipeline.Agent
import com.example.mindweaverstudio.data.model.pipeline.AgentPipelineData
import com.example.mindweaverstudio.data.model.pipeline.AgentResult

class AgentPipeline(
    private val agents: List<Agent<AgentPipelineData, AgentPipelineData>>
) {

    suspend fun run(
        initialInput: AgentPipelineData,
        onSuccess: (AgentResult) -> Unit,
        onStart: (String) -> Unit
    ) {
        var currentInput = initialInput

        for (agent in agents) {
            try {
                println("[${agent.name}] start")

                onStart(agent.name)
                val output = agent.run(currentInput)
                currentInput = output

                println("[${agent.name}] completed")

                onSuccess(AgentResult.Companion.createSuccess(output))
            } catch (exception: Exception) {

                println("[${agent.name}] failed: ${exception.message}")

                onSuccess(AgentResult.Companion.createFailure(exception, agent.name))
            }
        }
    }
}
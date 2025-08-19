package com.example.mindweaverstudio.data.agents

import com.example.mindweaverstudio.data.models.pipeline.Agent
import com.example.mindweaverstudio.data.models.pipeline.AgentPipelineData
import com.example.mindweaverstudio.data.models.pipeline.AgentPipelineData.Companion.createInitial
import com.example.mindweaverstudio.data.models.pipeline.AgentResult

class TextAnalyzerAgentPipeline(
    private val agents: List<Agent<AgentPipelineData, AgentPipelineData>>,
) {

    suspend fun run(
        prompt: String,
        onSuccess: (AgentResult) -> Unit,
        onStart: (String) -> Unit
    ) {
        var currentInput = createInitial(
            prompt = prompt,
            agentName = agents.firstOrNull()?.name.orEmpty()
        )

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
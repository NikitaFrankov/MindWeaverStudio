package com.example.mindweaverstudio.data.ai.pipelines

import com.example.mindweaverstudio.data.ai.memory.MemoryStore
import com.example.mindweaverstudio.data.ai.memory.getLastCompletedStep
import com.example.mindweaverstudio.data.ai.memory.getStepOutput
import com.example.mindweaverstudio.data.ai.memory.saveStepResult
import com.example.mindweaverstudio.data.ai.pipelines.common.Pipeline
import com.example.mindweaverstudio.data.models.chat.remote.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.PipelineOptions
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.errorPipelineResult

class PipelineRunner(
    private val memoryStore: MemoryStore
) {
    suspend fun execute(
        pipeline: Pipeline,
        input: ChatMessage,
        options: PipelineOptions
    ): PipelineResult {
        val steps = pipeline.steps()
        val lastCompletedStep = if (options.resumeFromLast) {
            try {
                memoryStore.getLastCompletedStep(pipeline.name) ?: 0
            } catch (e: Exception) {
                return errorPipelineResult(e)
            }
        } else 0

        var result: PipelineResult? = null

        for (step in steps) {
            if (step.id <= lastCompletedStep) continue

            val stepInput = if (step.id == 1) {
                input.content
            } else {
                val inpuuuut = result?.message
                    ?: memoryStore.getStepOutput(pipeline.name, step.id - 1)
                    ?: return errorPipelineResult("Missing input for step ${step.id} in ${pipeline.name}")
                inpuuuut
            }

            result = step.action(stepInput)

            try {
                memoryStore.saveStepResult(
                    pipelineName = pipeline.name,
                    stepId = step.id,
                    agentName = step.agentName,
                    result = result
                )
            } catch (e: Exception) {
                return errorPipelineResult(e)
            }

            if (result.isError) return result
        }

        return result ?: errorPipelineResult("Pipeline ${pipeline.name} has no steps")
    }
}
package com.example.mindweaverstudio.components.pipeline

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.data.model.pipeline.AgentResult

interface PipelineStore : Store<PipelineStore.Intent, PipelineStore.State, PipelineStore.Label> {

    data class State(
        val initialInput: String = "",
        val isRunning: Boolean = false,
        val results: List<AgentResult> = emptyList(),
        val error: String? = null,
        val executionLogs: List<String> = emptyList()
    )

    sealed class Intent {
        data class UpdateInput(val input: String) : Intent()
        data object RunPipeline : Intent()
        data object ClearResults : Intent()
        data object ClearError : Intent()
    }

    sealed class Label {
        data class ShowError(val message: String) : Label()
    }
}
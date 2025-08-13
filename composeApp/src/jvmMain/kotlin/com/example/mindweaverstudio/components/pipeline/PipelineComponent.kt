package com.example.mindweaverstudio.components.pipeline

import kotlinx.coroutines.flow.StateFlow

interface PipelineComponent {
    val state: StateFlow<PipelineStore.State>

    fun onIntent(intent: PipelineStore.Intent)
}
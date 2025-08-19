package com.example.mindweaverstudio.components.pipeline

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.data.models.pipeline.AgentResult
import com.example.mindweaverstudio.data.agents.TextAnalyzerAgentPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

class PipelineStoreFactory(
    private val storeFactory: StoreFactory,
    private val textAnalyzerAgentPipeline: TextAnalyzerAgentPipeline,
) {

    fun create(): PipelineStore =
        object : PipelineStore, Store<PipelineStore.Intent, PipelineStore.State, PipelineStore.Label> by storeFactory.create(
            name = "PipelineStore",
            initialState = PipelineStore.State(),
            bootstrapper = Bootstrapper(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Action

    private sealed class Msg {
        data class InputUpdated(val input: String) : Msg()
        data class RunningChanged(val isRunning: Boolean) : Msg()
        data class ResultsUpdated(val results: List<AgentResult>) : Msg()
        data class ResultUpdated(val results: AgentResult) : Msg()
        data class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
        data object ResultsCleared : Msg()
        data class LogAdded(val log: String) : Msg()
        data class LogsCleared(val logs: List<String>) : Msg()
    }

    private inner class Bootstrapper: CoroutineBootstrapper<Action>() {
        override fun invoke() {

        }
    }

    private inner class ExecutorImpl : CoroutineExecutor<PipelineStore.Intent, Action, PipelineStore.State, Msg, PipelineStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        override fun executeIntent(intent: PipelineStore.Intent) {
            when (intent) {
                is PipelineStore.Intent.UpdateInput -> dispatch(Msg.InputUpdated(intent.input))

                is PipelineStore.Intent.RunPipeline -> {
                    val currentState = state()
                    if (currentState.initialInput.isNotBlank() && !currentState.isRunning) {
                        runPipeline(currentState.initialInput)
                    }
                }

                is PipelineStore.Intent.ClearResults -> {
                    dispatch(Msg.ResultsCleared)
                    dispatch(Msg.LogsCleared(emptyList()))
                }

                is PipelineStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)
            }
        }

        private fun runPipeline(input: String) {
            dispatch(Msg.RunningChanged(true))
            dispatch(Msg.LogAdded("Pipeline execution started"))
            
            scope.launch {
                try {
                    textAnalyzerAgentPipeline.run(
                        prompt = input,
                        onStart = { agentName ->
                            dispatch(Msg.LogAdded("Agent \"$agentName\" start task"))
                        },
                        onSuccess = {
                            dispatch(Msg.ResultUpdated(it))
                            dispatch(Msg.LogAdded(it.successMessage))
                        }
                    )
                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Unknown error occurred"
                    dispatch(Msg.ErrorOccurred(errorMessage))
                    dispatch(Msg.LogAdded("Pipeline execution failed: $errorMessage"))
                    publish(PipelineStore.Label.ShowError(errorMessage))
                } finally {
                    dispatch(Msg.RunningChanged(false))
                }
            }
        }
    }

    private object ReducerImpl : Reducer<PipelineStore.State, Msg> {
        override fun PipelineStore.State.reduce(msg: Msg): PipelineStore.State =
            when (msg) {
                is Msg.InputUpdated -> copy(initialInput = msg.input)
                is Msg.RunningChanged -> copy(isRunning = msg.isRunning)
                is Msg.ResultsUpdated -> copy(results = msg.results)
                is Msg.ErrorOccurred -> copy(error = msg.error)
                is Msg.ErrorCleared -> copy(error = null)
                is Msg.ResultsCleared -> copy(results = emptyList())
                is Msg.LogAdded -> copy(executionLogs = executionLogs + msg.log)
                is Msg.LogsCleared -> copy(executionLogs = msg.logs)
                is Msg.ResultUpdated -> copy(
                    results = buildList {
                        addAll(results)
                        add(msg.results)
                    }
                )
            }
    }
}
package com.example.mindweaverstudio.components.codeeditor

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DefaultCodeEditorComponent(
    private val codeEditorStoreFactory: CodeEditorStoreFactory,
    private val project: Project,
    private val callbackHandler: (Callback) -> Unit,
    componentContext: ComponentContext,
) : CodeEditorComponent, ComponentContext by componentContext {

    private val componentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val store = instanceKeeper
        .getStore { codeEditorStoreFactory.create(project = project) }

    init {
        componentScope.launch {
            store.labels.collect { label ->
                when (label) {
                    is CodeEditorStore.Label.ShowGithubInfoInputDialog ->
                        callbackHandler.invoke(Callback.ShowRepoInfoInput(project = project))
                }
            }
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<CodeEditorStore.State> = store.stateFlow

    override fun onIntent(intent: CodeEditorStore.Intent) {
        store.accept(intent)
    }

    override fun onNavigateToUserConfiguration() {
        callbackHandler.invoke(Callback.ShowUserConfiguration)
    }
}
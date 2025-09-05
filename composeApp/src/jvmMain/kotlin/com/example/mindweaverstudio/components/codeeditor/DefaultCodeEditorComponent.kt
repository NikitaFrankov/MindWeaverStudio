package com.example.mindweaverstudio.components.codeeditor

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.mindweaverstudio.components.projectselection.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

class DefaultCodeEditorComponent(
    private val codeEditorStoreFactory: CodeEditorStoreFactory,
    componentContext: ComponentContext,
    private val project: Project,
    private val onNavigateToUserConfiguration: () -> Unit = {}
) : CodeEditorComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        codeEditorStoreFactory.create(project = project)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<CodeEditorStore.State> = store.stateFlow

    override fun onIntent(intent: CodeEditorStore.Intent) {
        store.accept(intent)
    }

    override fun onNavigateToUserConfiguration() {
        onNavigateToUserConfiguration.invoke()
    }
}
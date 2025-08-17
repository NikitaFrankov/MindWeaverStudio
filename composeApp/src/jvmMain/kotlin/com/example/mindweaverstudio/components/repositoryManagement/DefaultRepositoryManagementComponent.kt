package com.example.mindweaverstudio.components.repositoryManagement

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

class DefaultRepositoryManagementComponent(
    private val repositoryManagementStoreFactory: RepositoryManagementStoreFactory,
    componentContext: ComponentContext,
) : RepositoryManagementComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        repositoryManagementStoreFactory.create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<RepositoryManagementStore.State> = store.stateFlow

    override fun onIntent(intent: RepositoryManagementStore.Intent) {
        store.accept(intent)
    }
}
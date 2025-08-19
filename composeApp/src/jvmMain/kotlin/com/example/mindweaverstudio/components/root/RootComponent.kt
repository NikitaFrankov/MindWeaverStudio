package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.example.mindweaverstudio.components.pipeline.PipelineComponent
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun navigateToPipeline()
    fun navigateToRepositoryManagement()

    sealed interface Child {
        class Pipeline(val component: PipelineComponent) : Child
        class RepositoryManagement(val component: RepositoryManagementComponent) : Child
    }
}
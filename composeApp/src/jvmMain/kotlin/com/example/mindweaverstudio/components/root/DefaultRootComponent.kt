package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.example.mindweaverstudio.components.pipeline.PipelineComponent
import com.example.mindweaverstudio.components.pipeline.DefaultPipelineComponent
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementComponent
import com.example.mindweaverstudio.components.repositoryManagement.DefaultRepositoryManagementComponent
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent
import com.example.mindweaverstudio.components.codeeditor.DefaultCodeEditorComponent
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionComponent
import com.example.mindweaverstudio.components.projectselection.DefaultProjectSelectionComponent
import com.example.mindweaverstudio.components.projectselection.Project
import org.koin.core.component.KoinComponent
import com.example.mindweaverstudio.components.root.RootComponent.Child
import kotlinx.serialization.Serializable
import org.koin.core.component.get

class DefaultRootComponent(componentContext: ComponentContext) :
    RootComponent,
    KoinComponent,
    ComponentContext by componentContext
{

    /** Private properties */

    private val navigation = StackNavigation<Config>()

    /** Public properties */

    override val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.ProjectSelection,
        handleBackButton = true,
        childFactory = ::child
    )

    /** Private methods */

    private fun child(config: Config, componentContext: ComponentContext): Child {
        return when(config) {
            is Config.ProjectSelection -> Child.ProjectSelection(projectSelectionComponent(componentContext))
            is Config.Pipeline -> Child.Pipeline(pipelineComponent(componentContext))
            is Config.RepositoryManagement -> Child.RepositoryManagement(repositoryManagementComponent(componentContext))
            is Config.CodeEditor -> Child.CodeEditor(codeEditorComponent(
                componentContext = componentContext,
                project = config.project
            ))
        }
    }

    private fun projectSelectionComponent(componentContext: ComponentContext): ProjectSelectionComponent {
        return DefaultProjectSelectionComponent(
            projectSelectionStoreFactory = get(),
            componentContext = componentContext,
            onProjectSelected = { projectPath ->
                navigateToCodeEditor(projectPath)
            }
        )
    }

    private fun pipelineComponent(componentContext: ComponentContext): PipelineComponent {
        return DefaultPipelineComponent(
            pipelineStoreFactory = get(),
            componentContext = componentContext,
        )
    }

    private fun repositoryManagementComponent(componentContext: ComponentContext): RepositoryManagementComponent {
        return DefaultRepositoryManagementComponent(
            repositoryManagementStoreFactory = get(),
            componentContext = componentContext,
        )
    }

    private fun codeEditorComponent(
        componentContext: ComponentContext,
        project: Project,
    ): CodeEditorComponent {
        return DefaultCodeEditorComponent(
            componentContext = componentContext,
            codeEditorStoreFactory = get(),
            project = project,
        )
    }

    /** Child components callbacks */

    override fun navigateToProjectSelection() {
        navigation.navigate(
            transformer = { _: List<Config> -> listOf(Config.ProjectSelection) },
            onComplete = { _, _ -> }
        )
    }

    override fun navigateToPipeline() {
        navigation.navigate(
            transformer = { _: List<Config> -> listOf(Config.Pipeline) },
            onComplete = { _, _ -> }
        )
    }

    override fun navigateToRepositoryManagement() {
        navigation.navigate(
            transformer = { _: List<Config> -> listOf(Config.RepositoryManagement) },
            onComplete = { _, _ -> }
        )
    }

    override fun navigateToCodeEditor(project: Project) {
        navigation.bringToFront(Config.CodeEditor(project))
    }

    @Serializable
    private sealed interface Config {
        
        @Serializable
        data object ProjectSelection : Config
        
        @Serializable
        data object Pipeline : Config
        
        @Serializable
        data object RepositoryManagement : Config
        
        @Serializable
        class CodeEditor(val project: Project) : Config
    }
}

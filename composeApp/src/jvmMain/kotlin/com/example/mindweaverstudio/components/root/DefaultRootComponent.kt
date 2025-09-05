package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.example.mindweaverstudio.components.authentication.AuthenticationComponent
import com.example.mindweaverstudio.components.authentication.DefaultAuthenticationComponent
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent
import com.example.mindweaverstudio.components.codeeditor.DefaultCodeEditorComponent
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionComponent
import com.example.mindweaverstudio.components.projectselection.DefaultProjectSelectionComponent
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.components.userconfiguration.UserConfigurationComponent
import com.example.mindweaverstudio.components.userconfiguration.DefaultUserConfigurationComponent
import org.koin.core.component.KoinComponent
import com.example.mindweaverstudio.components.root.RootComponent.Child
import kotlinx.serialization.Serializable
import org.koin.core.component.get

class DefaultRootComponent(componentContext: ComponentContext) : RootComponent, KoinComponent, ComponentContext by componentContext {

    /** Private properties */

    private val navigation = StackNavigation<Config>()

    /** Public properties */

    override val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Authentication,
        handleBackButton = true,
        childFactory = ::child
    )

    /** Private methods */

    private fun child(config: Config, componentContext: ComponentContext): Child {
        return when(config) {
            is Config.Authentication -> Child.Authentication(authenticationComponent(componentContext))
            is Config.ProjectSelection -> Child.ProjectSelection(projectSelectionComponent(componentContext))
            is Config.CodeEditor -> Child.CodeEditor(codeEditorComponent(
                componentContext = componentContext,
                project = config.project
            ))
            is Config.UserConfiguration -> Child.UserConfiguration(userConfigurationComponent(componentContext))
        }
    }

    private fun authenticationComponent(componentContext: ComponentContext): AuthenticationComponent {
        return DefaultAuthenticationComponent(
            authenticationStoreFactory = get(),
            componentContext = componentContext,
            onAuthenticationSuccessful = {
                navigateToProjectSelection()
            }
        )
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

    private fun codeEditorComponent(
        componentContext: ComponentContext,
        project: Project,
    ): CodeEditorComponent {
        return DefaultCodeEditorComponent(
            componentContext = componentContext,
            codeEditorStoreFactory = get(),
            project = project,
            onNavigateToUserConfiguration = {
                navigateToUserConfiguration()
            }
        )
    }

    private fun userConfigurationComponent(componentContext: ComponentContext): UserConfigurationComponent {
        return DefaultUserConfigurationComponent(
            userConfigurationStoreFactory = get(),
            componentContext = componentContext,
            onNavigateBack = {
                navigateBack()
            }
        )
    }

    /** Child components callbacks */

    override fun navigateToAuthentication() {
        navigation.navigate(
            transformer = { _: List<Config> -> listOf(Config.Authentication) },
            onComplete = { _, _ -> }
        )
    }

    override fun navigateToProjectSelection() {
        navigation.navigate(
            transformer = { _: List<Config> -> listOf(Config.ProjectSelection) },
            onComplete = { _, _ -> }
        )
    }

    override fun navigateToCodeEditor(project: Project) {
        navigation.bringToFront(Config.CodeEditor(project))
    }

    override fun navigateToUserConfiguration() {
        navigation.bringToFront(Config.UserConfiguration)
    }

    override fun navigateBack() {
        navigation.pop()
    }

    @Serializable
    private sealed interface Config {
        
        @Serializable
        data object Authentication : Config
        
        @Serializable
        data object ProjectSelection : Config
        
        @Serializable
        class CodeEditor(val project: Project) : Config
        
        @Serializable
        data object UserConfiguration : Config
    }
}

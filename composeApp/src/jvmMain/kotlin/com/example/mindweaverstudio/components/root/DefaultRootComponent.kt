package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.example.mindweaverstudio.components.authentication.AuthenticationComponent
import com.example.mindweaverstudio.components.authentication.DefaultAuthenticationComponent
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent
import com.example.mindweaverstudio.components.codeeditor.DefaultCodeEditorComponent
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionComponent
import com.example.mindweaverstudio.components.projectselection.DefaultProjectSelectionComponent
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.components.repoInfoInput.DefaultRepoInfoInputComponent
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputComponent
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
            is Config.RepoInfoInput -> Child.RepoInfoInput(repoInfoInputComponent(componentContext))
        }
    }

    private fun authenticationComponent(componentContext: ComponentContext): AuthenticationComponent {
        return DefaultAuthenticationComponent(
            callbackHandler = ::handleAuthentificationCallbacks,
            componentContext = componentContext,
            authenticationStoreFactory = get(),
        )
    }

    private fun projectSelectionComponent(componentContext: ComponentContext): ProjectSelectionComponent {
        return DefaultProjectSelectionComponent(
            callbackHandler = ::handleProjectSelectionCallbacks,
            projectSelectionStoreFactory = get(),
            componentContext = componentContext,
        )
    }

    private fun codeEditorComponent(
        componentContext: ComponentContext,
        project: Project,
    ): CodeEditorComponent {
        return DefaultCodeEditorComponent(
            callbackHandler = ::handleCodeEditorCallbacks,
            componentContext = componentContext,
            codeEditorStoreFactory = get(),
            project = project,
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

    private fun repoInfoInputComponent(componentContext: ComponentContext): RepoInfoInputComponent {
        return DefaultRepoInfoInputComponent(
            componentContext = componentContext,
            storeFactory = get(),
            callbackHandler = ::handleRepoInfoInputCallbacks,
        )
    }

    /** Child components callbacks */

    private fun handleAuthentificationCallbacks(callback: AuthenticationComponent.Callback) = when(callback) {
        AuthenticationComponent.Callback.SuccessAuthentification ->
            navigation.bringToFront(Config.ProjectSelection)
    }

    private fun handleCodeEditorCallbacks(callback: CodeEditorComponent.Callback) = when(callback) {
        CodeEditorComponent.Callback.ShowUserConfiguration ->
            navigation.bringToFront(Config.UserConfiguration)

        CodeEditorComponent.Callback.ShowRepoInfoInput ->
            navigation.pushNew(Config.RepoInfoInput)
    }

    private fun handleProjectSelectionCallbacks(callback: ProjectSelectionComponent.Callback) = when(callback) {
        is ProjectSelectionComponent.Callback.ProjectSelected ->
            navigation.bringToFront(Config.CodeEditor(project = callback.project))
    }

    private fun handleRepoInfoInputCallbacks(callback: RepoInfoInputComponent.Callback) = when(callback) {
        is RepoInfoInputComponent.Callback.CloseDialog -> navigateBack()
    }

    private fun navigateBack() {
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

        @Serializable
        data object RepoInfoInput : Config
    }
}

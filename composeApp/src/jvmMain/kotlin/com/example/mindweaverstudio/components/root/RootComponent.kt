package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.example.mindweaverstudio.components.authentication.AuthenticationComponent
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionComponent
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputComponent
import com.example.mindweaverstudio.components.userconfiguration.UserConfigurationComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed interface Child {
        class Authentication(val component: AuthenticationComponent) : Child
        class ProjectSelection(val component: ProjectSelectionComponent) : Child
        class CodeEditor(val component: CodeEditorComponent) : Child
        class UserConfiguration(val component: UserConfigurationComponent) : Child
        class RepoInfoInput(val component: RepoInfoInputComponent) : Child
    }
}
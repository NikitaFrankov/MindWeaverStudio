package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun navigateToProjectSelection()
    fun navigateToCodeEditor(project: Project)

    sealed interface Child {
        class ProjectSelection(val component: ProjectSelectionComponent) : Child
        class CodeEditor(val component: CodeEditorComponent) : Child
    }
}
package com.example.mindweaverstudio.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.example.mindweaverstudio.components.root.RootComponent
import com.example.mindweaverstudio.components.root.RootComponent.Child
import com.example.mindweaverstudio.ui.screens.authentication.AuthenticationScreen
import com.example.mindweaverstudio.ui.screens.codeeditor.CodeEditorScreen
import com.example.mindweaverstudio.ui.screens.projectselection.ProjectSelectionScreen
import com.example.mindweaverstudio.ui.screens.repoInfoInput.RepoInfoInputDialog
import com.example.mindweaverstudio.ui.screens.userconfiguration.UserConfigurationScreen
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Composable
fun App(component: RootComponent) {
    MindWeaverTheme {
        RootContent(
            component = component,
            modifier = Modifier.background(MindWeaverTheme.colors.rootBackground)
        )
    }
}

@OptIn(ExperimentalAtomicApi::class)
@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        Children(
            stack = component.stack,
            modifier = modifier.padding(innerPadding),
            animation = stackAnimation(animator = fade() + scale()),
        ) {
            when (val child = it.instance) {
                is Child.Authentication -> AuthenticationScreen(child.component)
                is Child.ProjectSelection -> ProjectSelectionScreen(child.component)
                is Child.CodeEditor -> CodeEditorScreen(child.component)
                is Child.UserConfiguration -> UserConfigurationScreen(child.component)
                is Child.RepoInfoInput -> RepoInfoInputDialog(child.component)
            }
        }
    }
}
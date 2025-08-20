package com.example.mindweaverstudio.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.mindweaverstudio.components.root.RootComponent
import com.example.mindweaverstudio.components.root.RootComponent.Child
import com.example.mindweaverstudio.ui.pipeline.PipelineScreen
import com.example.mindweaverstudio.ui.repositoryManagement.RepositoryManagementScreen
import com.example.mindweaverstudio.ui.codeeditor.CodeEditorScreen
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Composable
fun App(component: RootComponent) {
    MaterialTheme {
        RootContent(
            component = component
        )
    }
}

@OptIn(ExperimentalAtomicApi::class)
@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    val stack by component.stack.subscribeAsState()
    
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                val currentChild = stack.active.instance
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Timeline, contentDescription = null) },
                    label = { Text("Pipeline") },
                    selected = currentChild is Child.Pipeline,
                    onClick = component::navigateToPipeline
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                    label = { Text("Repository") },
                    selected = currentChild is Child.RepositoryManagement,
                    onClick = component::navigateToRepositoryManagement
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    label = { Text("CodeEditor") },
                    selected = currentChild is Child.CodeEditor,
                    onClick = component::navigateToCodeEditor
                )
            }
        }
    ) { innerPadding ->
        Children(
            stack = component.stack,
            modifier = Modifier.padding(innerPadding),
            animation = stackAnimation(animator = fade())
        ) {
            when (val child = it.instance) {
                is Child.Pipeline -> PipelineScreen(child.component)
                is Child.RepositoryManagement -> RepositoryManagementScreen(child.component)
                is Child.CodeEditor -> CodeEditorScreen(child.component)
            }
        }
    }
}
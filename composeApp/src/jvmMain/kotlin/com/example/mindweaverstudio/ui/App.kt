package com.example.mindweaverstudio.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.example.mindweaverstudio.components.root.RootComponent
import com.example.mindweaverstudio.components.root.RootComponent.Child
import com.example.mindweaverstudio.ui.chat.ChatScreen
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
    Scaffold (
        modifier = modifier,
    ) { innerPadding ->
        Children(
            stack = component.stack,
            modifier = Modifier.padding(innerPadding),
            animation = stackAnimation(animator = fade())
        ) {
            when (val child = it.instance) {
                is Child.Chat -> ChatScreen(child.component)
            }
        }
    }
}
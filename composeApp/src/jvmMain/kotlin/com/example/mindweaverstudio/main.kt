package com.example.mindweaverstudio

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.mindweaverstudio.components.root.DefaultRootComponent
import com.example.mindweaverstudio.di.appModule
import com.example.mindweaverstudio.ui.app.App
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }

    application {
        val lifecycle = LifecycleRegistry()
        val root = DefaultRootComponent(componentContext = DefaultComponentContext(lifecycle = lifecycle),)

        val windowState = rememberWindowState()
        LifecycleController(lifecycle, windowState)

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "mindweaverstudio"
        ) {
            window.minimumSize = java.awt.Dimension(1800, 900)

            App(component = root)
        }
    }
}
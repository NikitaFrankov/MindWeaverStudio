package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.example.mindweaverstudio.components.chat.ChatComponent
import com.example.mindweaverstudio.components.pipeline.PipelineComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun navigateToChat()
    fun navigateToPipeline()

    sealed interface Child {
        class Chat(val component: ChatComponent) : Child
        class Pipeline(val component: PipelineComponent) : Child
    }
}
package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.example.mindweaverstudio.components.chat.ChatComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed interface Child {
        class Chat(val component: ChatComponent) : Child
    }
}
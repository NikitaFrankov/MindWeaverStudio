package com.example.mindweaverstudio.components.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.components.chat.ChatComponent
import com.example.mindweaverstudio.components.chat.DefaultChatComponent
import org.koin.core.component.KoinComponent
import com.example.mindweaverstudio.components.root.RootComponent.Child
import org.koin.core.component.get

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val storeFactory: StoreFactory = DefaultStoreFactory(),
) : RootComponent, KoinComponent, ComponentContext by componentContext {

    /** Private properties */

    private val navigation = StackNavigation<Config>()

    /** Public properties */

    override val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Chat,
        handleBackButton = true,
        childFactory = ::child
    )

    /** Private methods */

    private fun child(config: Config, componentContext: ComponentContext): Child {
        return when(config) {
            is Config.Chat -> Child.Chat(chatComponent(componentContext))
        }
    }

    private fun chatComponent(componentContext: ComponentContext): ChatComponent {
        return DefaultChatComponent(
            componentContext = componentContext,
            neuralNetworkRepository = get(),
            storeFactory = storeFactory,
        )
    }

    /** Child components callbacks */


    @kotlinx.serialization.Serializable
    private sealed interface Config {
        @kotlinx.serialization.Serializable
        data object Chat : Config
    }
}

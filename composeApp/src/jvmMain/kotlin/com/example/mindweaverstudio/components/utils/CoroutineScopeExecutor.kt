package com.example.mindweaverstudio.components.utils

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

open class CoroutineScopeExecutor<in Intent : Any, Action : Any, State : Any, Message : Any, Label : Any>(
    private val mainContext: CoroutineContext = Dispatchers.Main
) :
    CoroutineExecutor<Intent, Action, State, Message, Label>(mainContext = mainContext),
    CoroutineScope by CoroutineScope(mainContext)
package com.example.mindweaverstudio.data.interruptions

import com.example.mindweaverstudio.data.models.interruptions.Signal
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class InterruptionsBus {
    private val _interrupts = MutableSharedFlow<Signal>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        replay = 1
    )
    val interrupts: SharedFlow<Signal> = _interrupts.asSharedFlow()

    suspend fun emitInterruption(signal: Signal) {
        _interrupts.emit(signal)
    }
}
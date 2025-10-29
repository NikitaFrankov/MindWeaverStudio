
package com.example.mindweaverstudio.data.interruptions

import com.example.mindweaverstudio.data.models.interruptions.Signal
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

class SystemInterruptionsProvider(
    private val interruptionsBus: InterruptionsBus,
) {
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<Any?>>()


    suspend fun emitSinal(signal: Signal): Any? {
        interruptionsBus.emitInterruption(signal)

        val deferred = CompletableDeferred<Any?>().apply {
            invokeOnCompletion {
                pendingRequests.remove(signal.id)
            }
        }
        pendingRequests.put(signal.id, deferred)

        return try {
            deferred.await()
        } catch (e: CancellationException) {
            pendingRequests.remove(signal.id)
            throw e
        }
    }

    fun respondToSignal(signalId: String, rawResponse: Any?): Boolean {
        val deferred = pendingRequests[signalId] ?: return false
        val result = deferred.complete(rawResponse)
        pendingRequests.remove(signalId)

        return result
    }
}


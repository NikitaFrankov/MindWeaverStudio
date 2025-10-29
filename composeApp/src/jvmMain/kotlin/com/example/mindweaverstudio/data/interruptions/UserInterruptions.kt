package com.example.mindweaverstudio.data.interruptions

import com.example.mindweaverstudio.data.models.interruptions.Signal
import com.example.mindweaverstudio.data.models.interruptions.SignalType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
suspend fun SystemInterruptionsProvider.sendUserInformationRequest(request: String): String {
    val signal = Signal(
        id = Uuid.random().toString(),
        value = request,
        type = SignalType.USER_INFO_REQUEST
    )
    return emitSinal(signal) as? String? ?: "error during user info request"
}
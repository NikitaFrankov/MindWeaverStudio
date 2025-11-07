package com.example.mindweaverstudio.ai.tools.user

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.example.mindweaverstudio.data.interruptions.SystemInterruptionsProvider
import com.example.mindweaverstudio.data.interruptions.sendUserInformationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserInteractionTools(
    private val systemInterruptionsProvider: SystemInterruptionsProvider,
) : ToolSet {

    @Tool
    @LLMDescription("Ask the user for the necessary information")
    suspend fun askUser(
        @LLMDescription("information request")
        request: String,
    ): String {
        val result = withContext(Dispatchers.IO) {
            systemInterruptionsProvider.sendUserInformationRequest(request = request)
        }
        return result
    }

}
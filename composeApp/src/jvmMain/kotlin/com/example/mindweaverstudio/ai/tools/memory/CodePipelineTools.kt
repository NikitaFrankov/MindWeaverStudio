package com.example.mindweaverstudio.ai.tools.memory

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodePipelineTools(

) : ToolSet {

    @Tool
    @LLMDescription("Save fact in memory")
    suspend fun saveFactInMemory(
        @LLMDescription("Fact to save")
        fact: String
    ): String {
        return withContext(Dispatchers.Default) {
            ""
        }
    }
}
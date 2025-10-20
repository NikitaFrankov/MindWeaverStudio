package com.example.mindweaverstudio.data.ai.tools.codeCheck

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.example.mindweaverstudio.data.clients.DockerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodeCheckTools(
    private val dockerClient: DockerClient,
) : ToolSet {

    @Tool
    @LLMDescription("Run and check the code snippet in a Docker container")
    suspend fun checkCodeSnippet(
        @LLMDescription("The code snippet")
        code: String,
        @LLMDescription("Code language")
        language: String,
    ): String {
        val result = withContext(Dispatchers.IO) {
            dockerClient.checkCode(
                code = code,
                language = language,
            )
        }
        return result
    }

}
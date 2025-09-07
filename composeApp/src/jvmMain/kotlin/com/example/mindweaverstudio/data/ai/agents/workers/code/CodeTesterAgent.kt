package com.example.mindweaverstudio.data.ai.agents.workers.code

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.CODE_TESTER_AGENT
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.utils.codereplacer.CodeReplacerUtils
import kotlinx.serialization.json.Json

class CodeTesterAgent(
    private val aiClient: AiClient,
) : Agent {

    override val name = CODE_TESTER_AGENT
    override val description: String = "Агент, который тестирует переданный код в изолированном контейнере"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun run(input: String): PipelineResult {
        val inputObject: CodeFixerResult = json.decodeFromString<CodeFixerResult>(input)

        val result = CodeReplacerUtils.replaceCodeInFile(
            filePath = inputObject.filepath,
            originalCode = inputObject.sourceCode,
            newCode = inputObject.newCode
        )

        return PipelineResult.Companion.successPipelineResult(result)
    }
}
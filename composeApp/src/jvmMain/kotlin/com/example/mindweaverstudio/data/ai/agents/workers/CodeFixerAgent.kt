package com.example.mindweaverstudio.data.ai.agents.workers

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.aiClients.AiClient
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createErrorPipelineResult
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult.Companion.createSuccessPipelineResult
import com.example.mindweaverstudio.data.ai.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_USER
import com.example.mindweaverstudio.data.utils.sourcecode.SourceCodeFinder
import com.example.mindweaverstudio.data.utils.sourcecode.models.SearchResult
import com.example.mindweaverstudio.data.utils.sourcecode.models.getFirstMatch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class CodeFixerResult(
    val sourceCode: String,
    val filepath: String,
    val newCode: String
)

class CodeFixerAgent(
    private val aiClient: AiClient,
) : Agent {

    override val name = CODE_FIXER_AGENT
    override val description: String = "Агент, который фиксит баги."

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun run(input: String): PipelineResult {
        val className = findClassName(input).orEmpty()
        val codeSearch = findSourcesByQuery(className)
        val userMessageString = input.replace(className, codeSearch.getFirstMatch()?.sourceCode.orEmpty())
        val userMessage = ChatMessage(content = userMessageString, role = ROLE_USER)

        val systemPrompt = generateTestSystemPrompt()
        val messages = listOf(systemPrompt, userMessage)
        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.1,
            maxTokens = 2000,
        )

        return result.fold(
            onSuccess = { response ->
                codeSearch as SearchResult.Success
                val searchResult = codeSearch.matches.first()

                val result = CodeFixerResult(
                    sourceCode = searchResult.sourceCode,
                    filepath = searchResult.filePath,
                    newCode = response.message
                )
                val resultString = json.encodeToString(CodeFixerResult.serializer(), result)

                createSuccessPipelineResult(message = resultString)
            },
            onFailure = { error ->
                createErrorPipelineResult(error)
            }
        )
    }

    private suspend fun findSourcesByQuery(className: String): SearchResult {
        return SourceCodeFinder().findSourceCode(
            projectRoot = "/Users/nikitaradionov/IdeaProjects/MindWeaver Studio",
            targetName = className,
        )
    }

    fun findClassName(query: String): String? {
        // Ищем слово, которое начинается с большой буквы и состоит из букв, цифр или _
        val regex = Regex("""\b[A-Z][A-Za-z0-9_]*\b""")
        return regex.find(query)?.value
    }

    private suspend fun generateTestSystemPrompt(): ChatMessage {
        val prompt =  """
        Ты — старший разработчик и автоматический инструмент для исправления багов в коде.
        Твоя единственная цель — получить код с конкретной проблемой и исправить эту проблему, ни шагу дальше.
        На вход ты получаешь код и описание конкретного бага или проблемы.
        Ты возвращаешь только исправленный код, без объяснений, без комментариев, без MARKDOWN и без других предложений по улучшению или оптимизации.
        Если входной код уже исправлен, возвращай его без изменений.
        Не добавляй никакой дополнительной функциональности, импортов, библиотек или комментариев.
        Формат вывода — чистый исправленный код в исходном стиле и языке, без оберток, без блочного форматирования БЕЗ ФОРМАТИРОВАНИЯ MARKDOWN, КОД НУЖНО ВОЗВРАЩАТЬ В ФОРМАТЕ ТЕКСТА
        
        Пример работы. Вход:
            fun sum(a: Int, b: Int): Int {
                return a + b
            }
        Проблема: метод возвращает неверный результат для отрицательных чисел
        
        Выход:
            fun sum(a: Int, b: Int): Int {
                return a + b
            }
        
        Запомни, твоя задача только исправить конкретную проблему. Больше ничего.
            """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt
        )
    }
}

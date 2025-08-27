package com.example.mindweaverstudio.data.agents

import com.example.mindweaverstudio.data.models.agents.Agent
import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.models.agents.AgentResult
import com.example.mindweaverstudio.data.models.agents.AgentResult.Companion.createErrorAgentResult
import com.example.mindweaverstudio.data.models.agents.AgentResult.Companion.createSuccessAgentResult
import com.example.mindweaverstudio.data.models.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_SYSTEM
import com.example.mindweaverstudio.data.utils.sourcecode.SourceCodeFinder
import com.example.mindweaverstudio.data.utils.sourcecode.models.getFirstMatch

class CodeFixerAgent(
    private val aiClient: AiClient,
) : Agent {

    override val name = CODE_FIXER_AGENT
    override val description: String = "Агент, который фиксит баги."

    override suspend fun run(input: ChatMessage): AgentResult {
        val userMessageString = findSourcesByQuery(input.content)
        val userMessage = input.copy(content = userMessageString)

        val systemPrompt = generateTestSystemPrompt()
        val messages = listOf(systemPrompt, userMessage)

        val result = aiClient.createChatCompletion(
            messages = messages,
            temperature = 0.3,
            maxTokens = 2000,
        )

        return result.fold(
            onSuccess = { response ->
                createSuccessAgentResult(message = response.message)
            },
            onFailure = { error ->
                createErrorAgentResult(error)
            }
        )
    }

    private suspend fun findSourcesByQuery(input: String): String {
        val className = findClassName(input).orEmpty()
        val sources = SourceCodeFinder().findSourceCode(
            projectRoot = "/Users/nikitaradionov/IdeaProjects/MindWeaver Studio",
            targetName = className,
        ).getFirstMatch()?.sourceCode.orEmpty()

        return input.replace(className, sources)
    }

    fun findClassName(query: String): String? {
        // Ищем слово, которое начинается с большой буквы и состоит из букв, цифр или _
        val regex = Regex("""\b[A-Z][A-Za-z0-9_]*\b""")
        return regex.find(query)?.value
    }

    private suspend fun generateTestSystemPrompt(): ChatMessage {
        val prompt =  """
          Ты — старший разработчик и автоматический инструмент для исправления багов в коде. Твоя единственная цель — получить код с конкретной проблемой и исправить эту проблему, ни шагу дальше. На вход ты получаешь код и описание конкретного бага или проблемы. Ты возвращаешь только исправленный код, без объяснений, без комментариев, без маркдауна и без других предложений по улучшению или оптимизации. Если входной код уже исправлен, возвращай его без изменений. Не добавляй никакой дополнительной функциональности, импортов, библиотек или комментариев. Формат вывода — чистый исправленный код в исходном стиле и языке, без оберток.
        
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

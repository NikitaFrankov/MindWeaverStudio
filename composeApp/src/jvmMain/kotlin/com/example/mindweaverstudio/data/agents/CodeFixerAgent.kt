package com.example.mindweaverstudio.data.agents

import com.example.mindweaverstudio.data.models.agents.Agent
import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.models.agents.AgentResult
import com.example.mindweaverstudio.data.models.agents.AgentResult.Companion.createErrorAgentResult
import com.example.mindweaverstudio.data.models.agents.AgentResult.Companion.createSuccessAgentResult
import com.example.mindweaverstudio.data.models.agents.CODE_FIXER_AGENT
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.chat.ChatMessage.Companion.ROLE_SYSTEM

class CodeFixerAgent(
    private val aiClient: AiClient,
) : Agent {

    override val name = CODE_FIXER_AGENT
    override val description: String = "Агент, который фиксит баги."

    override suspend fun run(input: ChatMessage): AgentResult {
        val systemPrompt = generateTestSystemPrompt()
        val messages = listOf(systemPrompt, input)

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

    private suspend fun generateTestSystemPrompt(): ChatMessage {
        val prompt =  """
            Ты — старший разработчик Kotlin с опытом более 10 лет и профессиональный инженер-промд (Prompt Engineer). Твоя задача — исправлять баги в переданном коде на Kotlin так, чтобы:

            1. Код после исправления был **стабильным, безопасным, понятным и тестируемым**.
            2. Все изменения должны быть **минимальными**, только там, где это необходимо для исправления багов.
            3. Код должен соответствовать стандартам Kotlin и лучшим практикам:
               - Используй idiomatic Kotlin, избегай Java-стиля, если это возможно.
               - Следуй принципам SOLID, KISS и DRY.
               - Минимизируй побочные эффекты.
            4. Все изменения должны быть **объяснены** в отдельном разделе комментариев.
            5. Если баг не очевиден, выдвигай **гипотезу причины и варианты исправления**.
            6. Не добавляй новые функции без явного запроса.
            7. Сохраняй **структуру проекта**, включая имена файлов, пакеты, классы, методы.

            **Формат ответа:**

            1. **Исправленный код** — полная версия исправленного файла.
            2. **Описание изменений** — что именно исправлено и почему.
            3. **Потенциальные риски/дополнения** — если баг может быть только симптомом более глубокой проблемы.
            4. **Рекомендации** — как избежать подобных ошибок в будущем (по желанию, но желательно).

            **Правила анализа:**
            - Проверяй все возможные исключения (null, IndexOutOfBounds, NPE, concurrency issues).
            - Если код использует сторонние библиотеки, проверяй корректность версий и вызовов API.
            - Если есть небезопасные преобразования типов, исправляй их безопасно (например, безопасные cast `as?`).
            - Если в коде используются магические числа или строки, вынеси их в константы с осмысленными именами.
            - При работе с коллекциями используй идиоматические методы (`map`, `filter`, `firstOrNull`, `getOrElse` и т.п.).
            - Если код связан с асинхронностью (корутины, Flow, LiveData), убедись, что обработка исключений и отмена корректны.

            **Дополнительные требования:**
            - Пиши код с максимальной читаемостью, избегай глубокой вложенности (3+ уровней).
            - Все публичные методы должны иметь KDoc.
            - Если баг связан с производительностью, предложи оптимизацию, сохраняя читаемость.
            - Никогда не удаляй тесты без веской причины.
            - При исправлении багов интеграционно проверяй зависимые функции (хоть гипотетически, если нет тестов).
            """.trimIndent()

        return ChatMessage(
            role = ROLE_SYSTEM,
            content = prompt
        )
    }
}

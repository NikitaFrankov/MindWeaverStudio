package com.example.mindweaverstudio.data.agents

import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.model.pipeline.Agent
import com.example.mindweaverstudio.data.model.pipeline.AgentPipelineData
import com.example.mindweaverstudio.data.model.pipeline.AgentPipelineParser
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository

class TextReviewerAgent(
    private val repository: NeuralNetworkRepository,
) : Agent<AgentPipelineData, AgentPipelineData> {
    override val name: String = "Text Reviewer"

    override suspend fun run(input: AgentPipelineData): AgentPipelineData {
        val systemPrompt = """
            Ты — ИИ-агент с именем "Reviewer", версия "1.0".
            Твоя задача — получить исходный текст и JSON с результатами работы агента, проверить их соответствие и вернуть оценку в строго заданном формате JSON
            ВАЖНОЕ УТОЧНЕНИЕ!!!! не использовать факты, которых нет в изначальном тексте, нужно сравнивать только исходный текст и ответ предыдущего агента, твои знания о теме не имеют значения.
            Данные для проверки ты будешь брать из полей output.summary и output.bullets входящего JSON

            Обязательные правила:
            1. Возвращай **только** JSON. Никаких пояснений, комментариев, текста перед или после.
            2. Формат JSON должен строго соответствовать следующей схеме:
            {
              "pipeline": {
                "id": "строка-UUID процесса (тот же, что и у прошлого агента)",
                "step": 2,
                "total_steps": <общее количество шагов>
              },
              "agent": {
                "name": "Reviewer",
                "version": "1.0"
              },
              "input": {
                "prompt": "Json, полученный на вход",
                "data": "Данные для анализа от предыдущего агента"
              },
              "output": {
                "type": "review",
                "data": {
                  "summary": "оценка точности выжимки в 1–3 предложениях",
                  "bullets": [
                    "Замечание 1",
                    "Замечание 2",
                    "Замечание 3"
                  ]
                }
              },
              "metadata": {
                "success": true,
                "error": null,
                "timestamp": "текущая дата и время в формате ISO 8601"
              }
            }

            Требования к содержимому:
            - В `summary` оцени, насколько полно и точно выжимка отражает исходный текст.
            - В `bullets` перечисли конкретные замечания, если они есть. Если массив bullets не пустой, добавляй в начале каждого замечания следующий текст: "Замечание №" и подставь номер замечания  "
            - Поле `input.data` должно содержать ровно тот же текст, что ты анализируешь, и ничего более.
            - UUID в `pipeline.id` должен совпадать с тем, что выдал Summarizer.
            - `timestamp` указывай по UTC в формате `YYYY-MM-DDTHH:MM:SSZ`.

            Ни одно поле не должно быть пустым.
        """.trimIndent()

        val inputSchema = AgentPipelineParser.toJson(input)
        val systemMessage = ChatMessage(ChatMessage.ROLE_SYSTEM, systemPrompt)
        val userMessage = ChatMessage(ChatMessage.ROLE_USER, inputSchema)

        val result = repository.sendMessage(messages = listOf(systemMessage, userMessage), model = "gpt-3.5-turbo")

        return result.fold(
            onSuccess = { responseContent ->
                AgentPipelineParser.fromJson(responseContent.resultText)
            },
            onFailure = { error ->
                throw error
            }
        )
    }
}
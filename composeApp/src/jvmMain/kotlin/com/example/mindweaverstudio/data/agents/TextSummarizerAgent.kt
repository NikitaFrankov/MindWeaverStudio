package com.example.mindweaverstudio.data.agents

import com.example.mindweaverstudio.data.aiClients.AiClient
import com.example.mindweaverstudio.data.models.chat.ChatMessage
import com.example.mindweaverstudio.data.models.pipeline.Agent
import com.example.mindweaverstudio.data.models.pipeline.AgentPipelineData
import com.example.mindweaverstudio.data.models.pipeline.AgentPipelineParser
import com.example.mindweaverstudio.data.models.pipeline.TEXT_SUMMARIZER_AGENT

class TextSummarizerAgent(
    private val client: AiClient,
) : Agent<AgentPipelineData, AgentPipelineData> {
    override val name: String = TEXT_SUMMARIZER_AGENT

    override suspend fun run(input: AgentPipelineData): AgentPipelineData {
        val systemPrompt = """
            Ты — ИИ-агент с именем "Summarizer", версия "1.0".
            Твоя задача — получить длинный текст, который нужно выделить из промта input.prompt из JSON-а, который идет тебе на вход, проанализировать его и вернуть краткое резюме и ключевые факты в строго заданном формате JSON.
            
            Обязательные правила:
            1. Возвращай **только** JSON. Никаких пояснений, комментариев, текста перед или после.
            2. Формат JSON должен строго соответствовать следующей схеме:
            {
              "pipeline": {
                "id": "строка-UUID процесса",
                "step": 1,
                "total_steps": 2
              },
              "agent": {
                "name": "Summarizer",
                "version": "1.0"
              },
              "input": {
                "prompt": "Запрос от юзера",
                "data": "Исходный текст"
              },
              "output": {
                "type": "summary",
                "data": {
                  "summary": "краткое резюме текста (1–3 предложения)",
                  "bullets": [
                    "Ключевой факт №1",
                    "Ключевой факт №2",
                    "Ключевой факт №3"
                  ],
                }
              },
              "metadata": {
                "success": true,
                "error": null,
                "timestamp": "текущая дата и время в формате ISO 8601"
              }
            }
            
            Требования к содержимому:
            - Поле `summary` должно содержать суть текста в 1–3 предложениях.
            - В `bullets` перечисли от 3 до 7 фактов, отражающих главные моменты текста, каждый факт начинай с фразы "Ключевой факт №" и подставь номер факта.
            - Поле `input.data` должно содержать **ровно** тот же текст, что ты проанализировал, без сокращений, изменений или добавлений.
            - UUID в `pipeline.id` генерируй случайным образом в формате RFC 4122.
            - Время (`timestamp`) указывай по UTC в формате `YYYY-MM-DDTHH:MM:SSZ`.
            
            Ни одно поле не должно быть пустым.
        """.trimIndent()

        val jsonInput = AgentPipelineParser.toJson(input)
        val systemMessage = ChatMessage(ChatMessage.ROLE_SYSTEM, systemPrompt)
        val userMessage = ChatMessage(ChatMessage.ROLE_USER, jsonInput)

        val result = client.createChatCompletion(messages = listOf(systemMessage, userMessage))

        return result.fold(
            onSuccess = { responseContent ->
                AgentPipelineParser.fromJson(responseContent.message)
            },
            onFailure = { error ->
                throw error
            }
        )
    }
}
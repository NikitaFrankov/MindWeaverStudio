package com.example.mindweaverstudio.components.chat

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val deepSeekRepository: NeuralNetworkRepository,
    private val chatGPTRepository: NeuralNetworkRepository,
    private val geminiRepository: NeuralNetworkRepository
) {

    fun create(): ChatStore =
        object : ChatStore, Store<ChatStore.Intent, ChatStore.State, ChatStore.Label> by storeFactory.create(
            name = "ChatStore",
            initialState = ChatStore.State(),
            bootstrapper = BootstrapperImpl(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private sealed class Action

    private sealed class Msg {
        data class UpdateMessage(val message: String) : Msg()
        data object MessageSent : Msg()
        data class MessagesUpdated(val messages: List<ChatMessage>) : Msg()
        data class LoadingChanged(val isLoading: Boolean) : Msg()
        data class ErrorOccurred(val error: String) : Msg()
        data object ErrorCleared : Msg()
        data object ChatCleared : Msg()
        data class ModelChanged(val model: String) : Msg()
        data class ProviderChanged(val provider: String) : Msg()
    }

    private class BootstrapperImpl : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            // Initial setup if needed
        }
    }

    private inner class ExecutorImpl : CoroutineExecutor<ChatStore.Intent, Action, ChatStore.State, Msg, ChatStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        override fun executeIntent(intent: ChatStore.Intent) {
            when (intent) {
                is ChatStore.Intent.UpdateMessage -> dispatch(Msg.UpdateMessage(intent.message))

                is ChatStore.Intent.SendMessage -> {
                    val currentState = state()
                    if (currentState.currentMessage.isNotBlank() && !currentState.isLoading) {
                        sendMessage(currentState.currentMessage, currentState.messages, currentState.selectedModel)
                    }
                }

                is ChatStore.Intent.ClearError -> dispatch(Msg.ErrorCleared)

                is ChatStore.Intent.ClearChat -> dispatch(Msg.ChatCleared)

                is ChatStore.Intent.ChangeModel -> dispatch(Msg.ModelChanged(intent.model))

                is ChatStore.Intent.ChangeProvider -> dispatch(Msg.ProviderChanged(intent.provider))
            }
        }

        private fun sendMessage(message: String, currentMessages: List<ChatMessage>, model: String) {
            dispatch(Msg.LoadingChanged(true))
            dispatch(Msg.MessageSent)

            val promptHeader = generatePromptHeader(message)

            val userMessage = ChatMessage(ChatMessage.ROLE_USER, "$promptHeader\n\n$message")
            val updatedMessages = currentMessages + userMessage
            dispatch(Msg.MessagesUpdated(updatedMessages))

            scope.launch {
                val repository = when (state().selectedProvider) {
                    "DeepSeek" -> deepSeekRepository
                    "ChatGPT" -> chatGPTRepository
                    "Gemini" -> geminiRepository
                    else -> deepSeekRepository
                }
                
                val result = repository.sendMessage(updatedMessages, model)
                result.fold(
                    onSuccess = { response ->
                        val assistantMessage = ChatMessage(ChatMessage.ROLE_ASSISTANT, response.answer.value.jsonPrimitive.contentOrNull.orEmpty())
                        dispatch(Msg.MessagesUpdated(updatedMessages + assistantMessage))
                        dispatch(Msg.LoadingChanged(false))
                    },
                    onFailure = { error ->
                        dispatch(Msg.ErrorOccurred(error.message ?: "Unknown error occurred"))
                        dispatch(Msg.LoadingChanged(false))
                        publish(ChatStore.Label.ShowError(error.message ?: "Unknown error occurred"))
                    }
                )
            }
        }
    }

    private object ReducerImpl : Reducer<ChatStore.State, Msg> {
        override fun ChatStore.State.reduce(msg: Msg): ChatStore.State =
            when (msg) {
                is Msg.UpdateMessage -> copy(currentMessage = msg.message)
                is Msg.MessageSent -> copy(currentMessage = "")
                is Msg.MessagesUpdated -> copy(messages = msg.messages)
                is Msg.LoadingChanged -> copy(isLoading = msg.isLoading)
                is Msg.ErrorOccurred -> copy(error = msg.error)
                is Msg.ErrorCleared -> copy(error = null)
                is Msg.ChatCleared -> copy(messages = emptyList(), currentMessage = "", error = null)
                is Msg.ModelChanged -> copy(selectedModel = msg.model)
                is Msg.ProviderChanged -> copy(
                    selectedProvider = msg.provider,
                    selectedModel = when (msg.provider) {
                        "DeepSeek" -> "deepseek-chat"
                        "ChatGPT" -> "gpt-3.5-turbo"
                        "Gemini" -> "gemini-1.5-flash"
                        else -> "deepseek-chat"
                    }
                )
            }
    }

    fun generatePromptHeader(message: String): String {
        val open = "<<<JSON>>>"
        val close = "<<<END>>>"

        val sysRu = buildString {
            appendLine("Ты должен вернуть ТОЛЬКО корректный JSON между $open и $close.")
            appendLine("Не добавляй никаких объяснений, markdown-блоков с кодом или текста вне этих маркеров.")
            appendLine("JSON обязан строго соответствовать описанной ниже структуре и назначению полей.")
            appendLine()
            appendLine("Описание полей:")
            appendLine("""
- formatVersion (string): Версия формата вывода, всегда "1.0" на данный момент.
- type (string): Категория задачи или вопроса. Для математических вычислений используй "calculation". Другие возможные значения: "explanation", "comparison", "analysis".
- answer (object):
    - value: Краткий итоговый ответ на вопрос. Число, если результат числовой; строка — в остальных случаях.
    - type: Тип данных значения. Допустимые: "number", "string", "boolean".
- points (array of objects): Каждый элемент описывает важный шаг или факт, использованный для получения ответа.
    - kind: Тип пункта. Допустимые: "step" (шаг вычисления), "fact" (фактическая информация), "note" (важное примечание).
    - text: Человеко-понятное описание шага/факта/примечания. Кратко и ясно.
- summary (object):
    - text: Короткое или расширенное пояснение ответа на естественном языке.
    - length: Уровень детализации. Допустимые: "short", "medium", "long".
- meta (object):
    - confidence: Число от 0.0 до 1.0, отражающее степень уверенности в ответе.
    - source: Идентификатор модели или системы, сгенерировавшей ответ (например, "model-x").
    """.trimIndent())
            appendLine()
            appendLine("Правила:")
            appendLine("- Вывод должен быть корректным JSON.")
            appendLine("- Используй только указанные поля и описанную структуру.")
            appendLine("- Применяй строго правильные типы данных.")
            appendLine("- Никогда не добавляй комментарии или пояснения вне JSON.")
            appendLine("- Все строки должны быть заключены в двойные кавычки.")
            appendLine()
            appendLine("Пример:")
            appendLine(open)
            appendLine("""
{
  "formatVersion": "1.0",
  "type": "calculation",
  "answer": {"value": 8, "type": "number"},
  "points": [
    {"kind": "step", "text": "2 * 3 = 6"},
    {"kind": "step", "text": "6 + 2 = 8"}
  ],
  "summary": {"text": "итоговое значение равно 8.", "length": "short"},
  "meta": {"confidence": 0.98, "source": "model-x"}
}
    """.trimIndent())
            appendLine(close)
        }


        val sysEng = buildString {
            appendLine("You are to return ONLY valid JSON between $open and $close.")
            appendLine("Do not include any explanations, markdown code fences, or text outside these markers.")
            appendLine("The JSON must strictly match the specified structure and field purposes described below.")
            appendLine()
            appendLine("Field descriptions:")
            appendLine("""
- formatVersion (string): Version of the output format, always "1.0" for now.
- type (string): Category of the task or question. For mathematical calculations use "calculation". Other possible values: "explanation", "comparison", "analysis".
- answer (object):
    - value: The concise final answer to the question. Numeric if it's a number, string otherwise.
    - type: Data type of value. Allowed: "number", "string", "boolean".
- points (array of objects): Each item describes an important step or fact used to produce the answer.
    - kind: Nature of the point. Allowed: "step" (calculation step), "fact" (factual information), "note" (important note).
    - text: Human-readable text describing the step/fact/note. Keep it short and clear.
- summary (object):
    - text: Short or extended natural language explanation of the answer.
    - length: Level of detail. Allowed: "short", "medium", "long".
- meta (object):
    - confidence: Number from 0.0 to 1.0 estimating confidence in the answer.
    - source: Identifier of the model or system producing the answer (e.g., "model-x").
    """.trimIndent())
            appendLine()
            appendLine("Rules:")
            appendLine("- The output must be valid JSON.")
            appendLine("- Only include the specified fields, in the specified structure.")
            appendLine("- Use the correct data types exactly as described.")
            appendLine("- Never include commentary or explanation outside the JSON.")
            appendLine("- All strings must be double-quoted.")
            appendLine()
            appendLine("Example:")
            appendLine(open)
            appendLine("""
{
  "formatVersion": "1.0",
  "type": "calculation",
  "answer": {"value": 8, "type": "number"},
  "points": [
    {"kind": "step", "text": "2 * 3 = 6"},
    {"kind": "step", "text": "6 + 2 = 8"}
  ],
  "summary": {"text": "итоговое значение равно 8.", "length": "short"},
  "meta": {"confidence": 0.98, "source": "model-x"}
}
    """.trimIndent())
            appendLine(close)
        }

        return when(detectRuLanguage(message)) {
            true -> sysRu
            false -> sysEng
        }
    }

    fun detectRuLanguage(prompt: String): Boolean {
        return prompt.any { it in '\u0400'..'\u04FF' }
    }
}
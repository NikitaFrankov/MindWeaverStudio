package com.example.mindweaverstudio.components.chat

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository
import com.example.mindweaverstudio.ui.model.UiChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

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
        data class MessagesUpdated(val messages: List<UiChatMessage>) : Msg()
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

        private fun sendMessage(message: String, currentMessages: List<UiChatMessage>, model: String) {
            dispatch(Msg.LoadingChanged(true))
            dispatch(Msg.MessageSent)

            val promptHeader = generatePromptHeader(message)

            val userUiMessage = UiChatMessage.createUserMessage(message)
            val updatedUiMessages = currentMessages + userUiMessage
            dispatch(Msg.MessagesUpdated(updatedUiMessages))

            scope.launch {
                val repository = when (state().selectedProvider) {
                    "DeepSeek" -> deepSeekRepository
                    "ChatGPT" -> chatGPTRepository
                    "Gemini" -> geminiRepository
                    else -> deepSeekRepository
                }
                
                // Convert UI messages to API messages for the request
                val apiUserMessage = ChatMessage(ChatMessage.ROLE_USER, "$promptHeader\n\n$message")
                val apiMessages = currentMessages.map { it.toApiMessage() } + apiUserMessage
                
                val result = repository.sendMessage(apiMessages, model)
                result.fold(
                    onSuccess = { response ->
                        val assistantUiMessage = UiChatMessage.createAssistantMessage(response)
                        dispatch(Msg.MessagesUpdated(updatedUiMessages + assistantUiMessage))
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
- type (string): Категория задачи или вопроса. Для математических вычислений используй "calculation". Другие возможные значения: "explanation", "comparison", "analysis", "simple".
- answer (object):
    - value: Краткий итоговый ответ на вопрос. Число, если результат числовой; строка — в остальных случаях.
    - type: Тип данных значения. Допустимые: "number", "string", "boolean".
- points (array of objects): Каждый элемент описывает важный шаг или факт, использованный для получения ответа.  
  Может быть пустым массивом, если детали не требуются.
    - kind: Тип пункта. Допустимые: "step" (шаг вычисления), "fact" (фактическая информация), "note" (важное примечание).
    - text: Человеко-понятное описание шага/факта/примечания. Кратко и ясно.
- summary (object):
    - text: Короткое или расширенное пояснение ответа на естественном языке. Для простых вопросов допускается минимальный текст.
    - length: Уровень детализации. Допустимые: "short", "medium", "long". Для простых запросов используйте "short".
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
            appendLine("- Если вопрос простой или разговорный, возвращай минимально возможный ответ: пустой points, короткий summary с length \"short\".")
            appendLine()

            appendLine("Пример минимального ответа на простой вопрос:")
            appendLine(open)
            appendLine("""
{
  "formatVersion": "1.0",
  "type": "simple",
  "answer": {"value": "Привет!", "type": "string"},
  "points": [],
  "meta": {"confidence": 0.99, "source": "model-x"}
}
""".trimIndent())

            appendLine("Пример ответа на обычный или большой вопрос:")
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
            appendLine("You MUST return ONLY valid JSON strictly between $open and $close.")
            appendLine("Do NOT add any explanations, code markdown blocks, or text outside these markers.")
            appendLine("The JSON must strictly conform to the structure and field purposes described below.")
            appendLine()
            appendLine("Field descriptions:")
            appendLine("""
- formatVersion (string): Version of the output format, always \"1.0\" for now.
- type (string): Category of the task or question. Use \"calculation\" for math calculations. Other possible values: \"explanation\", \"comparison\", \"analysis\", \"simple\".
- answer (object):
    - value: A concise final answer to the question. A number if numeric result; a string otherwise.
    - type: Data type of the value. Allowed: \"number\", \"string\", \"boolean\".
- points (array of objects): Each item describes an important step or fact used to produce the answer.  
  Can be an empty array if no details are required.
    - kind: Type of the item. Allowed: \"step\" (calculation step), \"fact\" (factual information), \"note\" (important note).
    - text: Human-readable description of the step/fact/note. Keep it brief and clear.
- summary (object):
    - text: Short or extended natural language explanation of the answer. Minimal text allowed for simple questions.
    - length: Level of detail. Allowed: \"short\", \"medium\", \"long\". Use \"short\" for simple queries.
- meta (object):
    - confidence: Number from 0.0 to 1.0 indicating confidence in the answer.
    - source: Identifier of the model or system producing the answer (e.g., \"model-x\").
""".trimIndent())
            appendLine()
            appendLine("Rules:")
            appendLine("- The output must be valid JSON.")
            appendLine("- Use only the specified fields and described structure.")
            appendLine("- Apply strictly correct data types.")
            appendLine("- Never include comments or explanations outside the JSON.")
            appendLine("- All strings must be double-quoted.")
            appendLine("- For simple or conversational questions, return the minimal possible answer: empty points array, short summary with length \"short\".")
            appendLine()
            appendLine("Example of a minimal answer to a simple question:")
            appendLine(open)
            appendLine("""
{
  "formatVersion": "1.0",
  "type": "simple",
  "answer": {"value": "Hello!", "type": "string"},
  "points": [],
  "summary": {"text": "A greeting message.", "length": "short"},
  "meta": {"confidence": 0.99, "source": "model-x"}
}
""".trimIndent())
            appendLine()
            appendLine("Example of an answer to a regular or complex question:")
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
  "summary": {"text": "The final value is 8.", "length": "short"},
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
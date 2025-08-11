package com.example.mindweaverstudio.services

interface PromptHeaderService {
    fun generatePromptHeader(message: String): String
}

class DefaultPromptHeaderService : PromptHeaderService {
    
    override fun generatePromptHeader(message: String): String {
        return when (detectRussianLanguage(message)) {
            true -> generateRussianPrompt()
            false -> generateEnglishPrompt()
        }
    }
    
    private fun detectRussianLanguage(prompt: String): Boolean {
        return prompt.any { it in '\u0400'..'\u04FF' }
    }
    
    private fun generateRussianPrompt(): String = buildString {
        appendLine("Ты должен вернуть ТОЛЬКО корректный JSON между $JSON_OPEN и $JSON_CLOSE.")
        appendLine("Не добавляй никаких объяснений, markdown-блоков с кодом или текста вне этих маркеров.")
        appendLine("JSON обязан строго соответствовать описанной ниже структуре и назначению полей.")
        appendLine()
        appendLine(FIELD_DESCRIPTIONS_RU)
        appendLine()
        appendLine(RULES_RU)
        appendLine()
        appendLine(MINIMAL_EXAMPLE_RU)
        appendLine(REGULAR_EXAMPLE_RU)
    }
    
    private fun generateEnglishPrompt(): String = buildString {
        appendLine("You MUST return ONLY valid JSON strictly between $JSON_OPEN and $JSON_CLOSE.")
        appendLine("Do NOT add any explanations, code markdown blocks, or text outside these markers.")
        appendLine("The JSON must strictly conform to the structure and field purposes described below.")
        appendLine()
        appendLine(FIELD_DESCRIPTIONS_EN)
        appendLine()
        appendLine(RULES_EN)
        appendLine()
        appendLine(MINIMAL_EXAMPLE_EN)
        appendLine(REGULAR_EXAMPLE_EN)
    }
    
    companion object {
        private const val JSON_OPEN = "<<<JSON>>>"
        private const val JSON_CLOSE = "<<<END>>>"
        
        private const val FIELD_DESCRIPTIONS_RU = """Описание полей:
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
    - source: Идентификатор модели или системы, сгенерировавшей ответ (например, "model-x")."""
        
        private const val FIELD_DESCRIPTIONS_EN = """Field descriptions:
- formatVersion (string): Version of the output format, always "1.0" for now.
- type (string): Category of the task or question. Use "calculation" for math calculations. Other possible values: "explanation", "comparison", "analysis", "simple".
- answer (object):
    - value: A concise final answer to the question. A number if numeric result; a string otherwise.
    - type: Data type of the value. Allowed: "number", "string", "boolean".
- points (array of objects): Each item describes an important step or fact used to produce the answer.  
  Can be an empty array if no details are required.
    - kind: Type of the item. Allowed: "step" (calculation step), "fact" (factual information), "note" (important note).
    - text: Human-readable description of the step/fact/note. Keep it brief and clear.
- summary (object):
    - text: Short or extended natural language explanation of the answer. Minimal text allowed for simple questions.
    - length: Level of detail. Allowed: "short", "medium", "long". Use "short" for simple queries.
- meta (object):
    - confidence: Number from 0.0 to 1.0 indicating confidence in the answer.
    - source: Identifier of the model or system producing the answer (e.g., "model-x")."""
        
        private const val RULES_RU = """Правила:
- Вывод должен быть корректным JSON.
- Используй только указанные поля и описанную структуру.
- Применяй строго правильные типы данных.
- Никогда не добавляй комментарии или пояснения вне JSON.
- Все строки должны быть заключены в двойные кавычки.
- Если вопрос простой или разговорный, возвращай минимально возможный ответ: пустой points, короткий summary с length "short"."""
        
        private const val RULES_EN = """Rules:
- The output must be valid JSON.
- Use only the specified fields and described structure.
- Apply strictly correct data types.
- Never include comments or explanations outside the JSON.
- All strings must be double-quoted.
- For simple or conversational questions, return the minimal possible answer: empty points array, short summary with length "short"."""
        
        private const val MINIMAL_EXAMPLE_RU = """Пример минимального ответа на простой вопрос:
$JSON_OPEN
{
  "formatVersion": "1.0",
  "type": "simple",
  "answer": {"value": "Привет!", "type": "string"},
  "points": [],
  "summary": {"text": "Приветственное сообщение.", "length": "short"},
  "meta": {"confidence": 0.99, "source": "model-x"}
}
$JSON_CLOSE"""
        
        private const val MINIMAL_EXAMPLE_EN = """Example of a minimal answer to a simple question:
$JSON_OPEN
{
  "formatVersion": "1.0",
  "type": "simple",
  "answer": {"value": "Hello!", "type": "string"},
  "points": [],
  "summary": {"text": "A greeting message.", "length": "short"},
  "meta": {"confidence": 0.99, "source": "model-x"}
}
$JSON_CLOSE"""
        
        private const val REGULAR_EXAMPLE_RU = """Пример ответа на обычный или большой вопрос:
$JSON_OPEN
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
$JSON_CLOSE"""
        
        private const val REGULAR_EXAMPLE_EN = """Example of an answer to a regular or complex question:
$JSON_OPEN
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
$JSON_CLOSE"""
    }
}
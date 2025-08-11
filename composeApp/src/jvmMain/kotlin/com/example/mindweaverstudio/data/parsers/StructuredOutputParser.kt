package com.example.mindweaverstudio.data.parsers

import com.example.mindweaverstudio.data.model.chat.StructuredOutput
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.JsonPrimitive

class StructuredOutputParser {
    fun parseStructuredJson(
        raw: String,
        open: String = "<<<JSON>>>",
        close: String = "<<<END>>>"
    ): StructuredOutput {
        val candidate = extractBetween(raw, open, close)
            ?: extractCodeFence(raw)
            ?: findFirstBalancedJson(raw)
            ?: throw IllegalArgumentException("No JSON block found")

        val parsed = try {
            decodeFromString<StructuredOutput>(candidate)
        } catch (e: Exception) {
            throw IllegalArgumentException("JSON parsing error: ${e.message}")
        }

        val errs = validateStructured(parsed)
        if (errs.isNotEmpty()) throw IllegalArgumentException("Validation errors: ${errs.joinToString("; ")}")
        return parsed
    }

    fun extractBetween(text: String, open: String, close: String): String? {
        val s = text.indexOf(open).takeIf { it >= 0 } ?: return null
        val e = text.indexOf(close, s + open.length).takeIf { it > s } ?: return null
        return text.substring(s + open.length, e).trim()
    }

    fun extractCodeFence(text: String): String? {
        val regex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        return regex.find(text)?.groups?.get(1)?.value?.trim()
    }

    /** Минимальная state-machine для поиска первого валидного JSON-объекта/массива */
    fun findFirstBalancedJson(text: String): String? {
        var i = 0
        while (i < text.length) {
            if (text[i] == '{' || text[i] == '[') {
                var depth = 0
                var inString = false
                var esc = false
                var j = i
                while (j < text.length) {
                    val c = text[j]
                    if (inString) {
                        if (esc) esc = false
                        else if (c == '\\') esc = true
                        else if (c == '"') inString = false
                    } else {
                        if (c == '"') inString = true
                        else if (c == '{' || c == '[') depth++
                        else if (c == '}' || c == ']') {
                            depth--
                            if (depth == 0) {
                                return text.substring(i, j + 1).trim()
                            }
                        }
                    }
                    j++
                }
            }
            i++
        }
        return null
    }

    /** Простая бизнес-валидация по правилам промпта */
    fun validateStructured(s: StructuredOutput): List<String> {
        val errs = mutableListOf<String>()
        if (s.formatVersion != "1.0") errs += "formatVersion must be \"1.0\""
        val allowedTypes = setOf("calculation", "explanation", "comparison", "analysis")
        if (s.type !in allowedTypes) errs += "type must be one of $allowedTypes"

        val allowedAnswerTypes = setOf("number", "string", "boolean")
        if (s.answer.type !in allowedAnswerTypes) errs += "answer.type must be one of $allowedAnswerTypes"

        // check that answer.value matches declared type
        val valElem = s.answer.value
        when (s.answer.type) {
            "number" -> if (valElem !is JsonPrimitive || !valElem.isStringOrNumber()) errs += "answer.value must be a number"
            "string" -> if (valElem !is JsonPrimitive || valElem.isString().not()) errs += "answer.value must be a string"
            "boolean" -> if (valElem !is JsonPrimitive || valElem.booleanOrNull == null) errs += "answer.value must be a boolean"
        }

        val allowedKinds = setOf("step", "fact", "note")
        s.points.forEachIndexed { idx, p ->
            if (p.kind !in allowedKinds) errs += "points[$idx].kind must be one of $allowedKinds"
            if (p.text.isBlank()) errs += "points[$idx].text must not be empty"
        }

        val allowedLengths = setOf("short", "medium", "long")
        if (s.summary.length !in allowedLengths) errs += "summary.length must be one of $allowedLengths"
        if (s.summary.text.isBlank()) errs += "summary.text must not be empty"

        s.meta?.confidence?.let {
            if (it < 0.0 || it > 1.0) errs += "meta.confidence must be in [0.0,1.0]"
        }
        return errs
    }

    /* JsonPrimitive helpers */
    private fun JsonPrimitive.isString(): Boolean = content != null && !isStringOrNumber()
    private fun JsonPrimitive.isStringOrNumber(): Boolean = try { content.toDouble(); true } catch (_: Exception) { false }
    private val JsonPrimitive.booleanOrNull: Boolean? get() = when (content.lowercase()) {
        "true" -> true; "false" -> false; else -> null
    }
}
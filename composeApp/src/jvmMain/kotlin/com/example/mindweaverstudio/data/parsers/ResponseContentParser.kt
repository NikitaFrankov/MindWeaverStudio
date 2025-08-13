package com.example.mindweaverstudio.data.parsers

import com.example.mindweaverstudio.data.model.chat.ResponseContent
import com.example.mindweaverstudio.data.model.chat.RequirementsSummary
import kotlinx.serialization.json.Json

class ResponseContentParser(
    private val structuredOutputParser: StructuredOutputParser
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    fun parseResponse(rawResponse: String): ResponseContent {
        // Try structured output first (has specific markers)
        if (containsStructuredMarkers(rawResponse)) {
            return try {
                val structuredOutput = structuredOutputParser.parseStructuredJson(rawResponse)
                ResponseContent.Structured(structuredOutput)
            } catch (e: Exception) {
                ResponseContent.PlainText(rawResponse)
            }
        }
        
        // Try requirements summary JSON (look for schema_version)
        val jsonContent = extractPossibleJson(rawResponse)
        if (jsonContent != null && jsonContent.contains("\"schema_version\"")) {
            return try {
                val summary = json.decodeFromString<RequirementsSummary>(jsonContent)
                ResponseContent.RequirementsSummary(summary)
            } catch (e: Exception) {
                ResponseContent.PlainText(rawResponse)
            }
        }
        
        // Default to plain text
        return ResponseContent.PlainText(rawResponse)
    }
    
    private fun containsStructuredMarkers(text: String): Boolean {
        return text.contains("<<<JSON>>>") && text.contains("<<<END>>>")
    }
    
    private fun extractPossibleJson(text: String): String? {
        // Try to extract JSON from code blocks
        val codeFenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        codeFenceRegex.find(text)?.groups?.get(1)?.value?.trim()?.let { return it }
        
        // Try to find balanced JSON
        return findFirstBalancedJson(text)
    }
    
    private fun findFirstBalancedJson(text: String): String? {
        var i = 0
        while (i < text.length) {
            if (text[i] == '{') {
                var depth = 0
                var inString = false
                var escape = false
                var j = i
                
                while (j < text.length) {
                    val char = text[j]
                    
                    if (escape) {
                        escape = false
                    } else if (char == '\\' && inString) {
                        escape = true
                    } else if (char == '"') {
                        inString = !inString
                    } else if (!inString) {
                        when (char) {
                            '{' -> depth++
                            '}' -> depth--
                        }
                        
                        if (depth == 0) {
                            val candidate = text.substring(i, j + 1).trim()
                            if (candidate.isNotEmpty()) return candidate
                        }
                    }
                    j++
                }
            }
            i++
        }
        return null
    }
}
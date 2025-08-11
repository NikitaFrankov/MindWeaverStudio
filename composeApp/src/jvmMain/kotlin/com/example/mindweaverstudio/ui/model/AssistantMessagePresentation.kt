package com.example.mindweaverstudio.ui.model

import androidx.compose.ui.graphics.Color
import com.example.mindweaverstudio.data.model.chat.StructuredOutput
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

data class AssistantMessagePresentation(
    val answerText: String,
    val steps: List<StepPoint>,
    val factAndPoints: List<FactPoint>,
    val summaryText: String,
    val confidence: String?,
    val source: String?
) {
    
    data class StepPoint(
        val text: String,
        val color: Color = Color(0xFF4CAF50)
    )
    
    data class FactPoint(
        val text: String,
        val color: Color
    )
    
    companion object {
        fun from(structuredOutput: StructuredOutput): AssistantMessagePresentation {
            val answerText = when (structuredOutput.answer.type) {
                "number" -> structuredOutput.answer.value.jsonPrimitive.doubleOrNull?.toString() 
                    ?: structuredOutput.answer.value.toString()
                "string" -> structuredOutput.answer.value.jsonPrimitive.contentOrNull 
                    ?: structuredOutput.answer.value.toString()
                "boolean" -> structuredOutput.answer.value.jsonPrimitive.booleanOrNull?.toString() 
                    ?: structuredOutput.answer.value.toString()
                else -> structuredOutput.answer.value.toString()
            }
            
            val steps = structuredOutput.steps.map { point ->
                StepPoint(point.text)
            }
            
            val factAndPoints = structuredOutput.factAndPoints.map { point ->
                val color = when (point.kind) {
                    "fact" -> Color.Blue
                    "note" -> Color.Magenta
                    else -> Color.Gray
                }
                FactPoint(point.text, color)
            }
            
            val confidence = structuredOutput.meta?.confidence?.let { conf ->
                "${(conf * 100).toInt()}%"
            }
            
            return AssistantMessagePresentation(
                answerText = answerText,
                steps = steps,
                factAndPoints = factAndPoints,
                summaryText = structuredOutput.summary.text,
                confidence = confidence,
                source = structuredOutput.meta?.source
            )
        }
    }
}
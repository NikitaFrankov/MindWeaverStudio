package com.example.mindweaverstudio.ui.chat

import com.example.mindweaverstudio.data.model.AppLocale

object PromptModeStrings {
    
    data class ModeLabels(
        val displayName: String,
        val description: String
    )
    
    private val structuredOutputLabels = mapOf(
        AppLocale.ENGLISH to ModeLabels(
            displayName = "Structured Output",
            description = "AI provides structured JSON responses with detailed analysis"
        ),
        AppLocale.RUSSIAN to ModeLabels(
            displayName = "Структурированный вывод",
            description = "ИИ предоставляет структурированные JSON-ответы с подробным анализом"
        )
    )
    
    private val requirementsGatheringLabels = mapOf(
        AppLocale.ENGLISH to ModeLabels(
            displayName = "Requirements Gathering",
            description = "AI systematically gathers project requirements through targeted questions"
        ),
        AppLocale.RUSSIAN to ModeLabels(
            displayName = "Сбор требований",
            description = "ИИ систематически собирает требования к проекту через целевые вопросы"
        )
    )
    
    fun getStructuredOutputLabels(locale: AppLocale): ModeLabels {
        return structuredOutputLabels[locale] ?: structuredOutputLabels[AppLocale.ENGLISH]!!
    }
    
    fun getRequirementsGatheringLabels(locale: AppLocale): ModeLabels {
        return requirementsGatheringLabels[locale] ?: requirementsGatheringLabels[AppLocale.ENGLISH]!!
    }
}
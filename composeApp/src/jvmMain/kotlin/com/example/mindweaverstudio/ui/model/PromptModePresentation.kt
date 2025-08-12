package com.example.mindweaverstudio.ui.model

import com.example.mindweaverstudio.data.model.AppLocale
import com.example.mindweaverstudio.data.model.PromptMode
import com.example.mindweaverstudio.ui.chat.PromptModeStrings

data class PromptModePresentation(
    val id: String,
    val displayName: String,
    val description: String,
    val systemPrompt: String
) {
    companion object {
        fun fromPromptMode(promptMode: PromptMode, locale: AppLocale): PromptModePresentation {
            val labels = when (promptMode.id) {
                "structured_output" -> PromptModeStrings.getStructuredOutputLabels(locale)
                "requirements_gathering" -> PromptModeStrings.getRequirementsGatheringLabels(locale)
                else -> PromptModeStrings.getStructuredOutputLabels(locale)
            }
            
            return PromptModePresentation(
                id = promptMode.id,
                displayName = labels.displayName,
                description = labels.description,
                systemPrompt = promptMode.systemPrompt
            )
        }
        
        fun getAllLocalizedModes(locale: AppLocale): List<PromptModePresentation> {
            return PromptMode.getAllModes().map { mode ->
                fromPromptMode(mode, locale)
            }
        }
    }
}
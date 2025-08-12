package com.example.mindweaverstudio.services

import com.example.mindweaverstudio.data.model.AppLocale
import com.example.mindweaverstudio.data.model.PromptMode
import com.example.mindweaverstudio.ui.model.PromptModePresentation

interface SystemPromptService {
    fun getSystemPrompt(promptModeId: String): String
    fun getAvailableModes(locale: AppLocale = AppLocale.getDefault()): List<PromptModePresentation>
}

class DefaultSystemPromptService : SystemPromptService {
    
    override fun getSystemPrompt(promptModeId: String): String {
        return PromptMode.getModeById(promptModeId).systemPrompt
    }
    
    override fun getAvailableModes(locale: AppLocale): List<PromptModePresentation> {
        return PromptModePresentation.getAllLocalizedModes(locale)
    }
}
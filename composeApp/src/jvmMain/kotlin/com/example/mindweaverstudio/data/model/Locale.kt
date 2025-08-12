package com.example.mindweaverstudio.data.model

enum class AppLocale(val code: String) {
    ENGLISH("en"),
    RUSSIAN("ru");
    
    companion object {
        fun detectFromText(text: String): AppLocale {
            return if (text.any { it in '\u0400'..'\u04FF' }) RUSSIAN else ENGLISH
        }
        
        fun getDefault(): AppLocale = ENGLISH
    }
}
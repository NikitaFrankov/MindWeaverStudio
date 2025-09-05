package com.example.mindweaverstudio.data.profile

import com.example.mindweaverstudio.data.models.profile.UserPersonalization
import kotlinx.serialization.json.Json
import java.io.File

object PersonalizationConfig {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val configFile = File(System.getProperty("user.home") + "/MindWeaverStudio/user_config.json").apply {
        parentFile.mkdirs()  // Создаём директорию, если нет
        if (!exists()) createNewFile()  // Создаём пустой файл
    }

    fun load(): UserPersonalization {
        val content = configFile.readText()
        return if (content.isNotBlank()) {
            json.decodeFromString(UserPersonalization.serializer(), content)
        } else {
            UserPersonalization()  // Дефолтные значения
        }
    }

    fun loadJsonConfig(): String = json.encodeToString(UserPersonalization.serializer(), load())

    fun save(config: UserPersonalization) {
        configFile.writeText(json.encodeToString(UserPersonalization.serializer(), config))
    }
}
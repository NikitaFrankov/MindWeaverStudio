package com.example.mindweaverstudio.data.extensions

import com.example.mindweaverstudio.data.models.mcp.base.ToolType
import io.modelcontextprotocol.kotlin.sdk.Tool

fun Tool.getToolReportFormat(): String {
    return when(ToolType.valueeOf(name)) {
        ToolType.UNKNOWN -> ""
        ToolType.FETCH_COMMITS -> ""
        ToolType.RUN_PROJECT_CONTAINER -> runProjectContainerReportFormat
    }
}

private val runProjectContainerReportFormat = """
    ОТЧЕТ MCP-СЕРВЕРА
    Дата: <YYYY-MM-DD HH:MM>

    1. Инициализация
       - Сервер и клиент инициализированы: <сервер/клиент, версии>
       - Зарегистрированные возможности: <capabilities>

    2. Инструменты
       - Список инструментов: <названия>

    3. Вызовы инструментов
       - Инструмент: ${ToolType.RUN_PROJECT_CONTAINER.value}
         * Результат вызова: успешно / ошибка

    4. Отправка отчета
       - Статус отправки в Telegram: успешно / ошибка

    Правила:
    - Не раскрывать содержимое контейнера, только статус вызова
    - Использовать краткие, сухие формулировки
    - Нумерация 1., 2., вложенные пункты через *
    - Не добавлять субъективные оценки
    - Не добавлять форматирование Markdown
""".trimIndent()
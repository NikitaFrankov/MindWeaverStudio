package com.example.mindweaverstudio.data.utils.extensions

import kotlinx.serialization.json.Json
import java.lang.Exception

inline fun <reified T> Json.decodeFromStringOrNull(jsonStr: String): T? =
    try {
        decodeFromString<T>(jsonStr)
    } catch (e: Exception) {
        null
    }
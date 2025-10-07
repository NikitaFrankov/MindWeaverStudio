package com.example.mindweaverstudio.data.settings

/*
Desktop implementation of a settings store compatible with the public API shape of
com.russhwolf:multiplatform-settings (basic primitives + observation) for
Compose Multiplatform desktop (JVM).

Features:
 - stores values in a small JSON file (atomic write)
 - supports String, Int, Long, Float, Boolean
 - thread-safe (Mutex)
 - emits updates via Kotlin Flow for observation
 - small and dependency-light (kotlinx-serialization, coroutines)

Usage:
 - add deps to desktopMain: kotlinx-serialization-json, kotlinx-coroutines-core
 - copy this file into desktopMain/kotlin (or include as a module)

NOTE: This is intentionally a standalone drop-in-like implementation; it doesn't
inherit the exact library interfaces to avoid binary incompatibilities — but the
public API below mirrors the common functions you expect from russhwolf's
Settings (get/put/remove/hasKey/clear + observe flows).
*/

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// ----------------------------
// Serialization model
// ----------------------------
@Serializable
private data class StoredEntry(val type: String, val value: String)

private val mapSerializer: KSerializer<Map<String, StoredEntry>> =
    MapSerializer(String.serializer(), StoredEntry.serializer())

private val json = Json { prettyPrint = false; encodeDefaults = true }

// ----------------------------
// DesktopSettings
// ----------------------------
class Settings(
    private val file: File,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val mutex = Mutex()

    // in-memory cache
    private val backing: MutableMap<String, StoredEntry> = mutableMapOf()

    // change emitter: emits keys that changed
    private val _changes = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val changes: Flow<String> get() = _changes

    init {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        } else {
            try {
                val text = file.readText()
                if (text.isNotEmpty()) {
                    val map = json.decodeFromString(mapSerializer, text)
                    backing.putAll(map)
                }
            } catch (e: Exception) {
                // If file corrupted, ignore - start empty. You may want to log.
            }
        }
    }

    // ----------------------------
    // Basic operations
    // ----------------------------
    suspend fun clear() {
        mutex.withLock {
            backing.clear()
            persistSync()
            _changes.tryEmit(ALL_KEYS)
        }
    }

    suspend fun remove(key: SettingsKey) {
        mutex.withLock {
            val removed = backing.remove(key.value)
            if (removed != null) {
                persistSync()
                _changes.tryEmit(key.value)
            }
        }
    }

    suspend fun hasKey(key: String): Boolean = mutex.withLock { backing.containsKey(key) }

    // String
    suspend fun putString(key: SettingsKey, value: String) = putTyped(key.value, "string", value)
    suspend fun getString(key: SettingsKey, default: String = ""): String = getTyped(key.value, "string")?.value ?: default
    suspend fun getStringOrNull(key: String): String? = getTyped(key, "string")?.value

    // Int
    suspend fun putInt(key: String, value: Int) = putTyped(key, "int", value.toString())
    suspend fun getInt(key: String, default: Int): Int = getTyped(key, "int")?.value?.toIntOrNull() ?: default

    // Long
    suspend fun putLong(key: String, value: Long) = putTyped(key, "long", value.toString())
    suspend fun getLong(key: String, default: Long): Long = getTyped(key, "long")?.value?.toLongOrNull() ?: default

    // Float
    suspend fun putFloat(key: String, value: Float) = putTyped(key, "float", value.toString())
    suspend fun getFloat(key: String, default: Float): Float = getTyped(key, "float")?.value?.toFloatOrNull() ?: default

    // Boolean
    suspend fun putBoolean(key: String, value: Boolean) = putTyped(key, "boolean", value.toString())
    suspend fun getBoolean(key: String, default: Boolean): Boolean = getTyped(key, "boolean")?.value?.toBoolean() ?: default

    // Observe as Flow<String> emitting the value serialized as string. You can map/convert in callers.
    fun observe(key: String): Flow<String?> =
        changes.filter { it == key || it == ALL_KEYS }.map {
            // read current value on collector side - but we need to avoid suspending here, so do IO in coroutine
            // To keep API simple, the user can call getX from a coroutine; however we also allow reading current value synchronously from cache
            synchronized(backing) {
                backing[key]?.value
            }
        }

    // Helper for convenience: observe typed values
    fun observeString(key: String, default: String): Flow<String> =
        observe(key).map { it ?: default }

    // ----------------------------
    // Internal helpers
    // ----------------------------
    private suspend fun putTyped(key: String, type: String, value: String) {
        mutex.withLock {
            backing[key] = StoredEntry(type, value)
            persistSync()
            _changes.tryEmit(key)
        }
    }

    private suspend fun getTyped(key: String, expectedType: String): StoredEntry? = mutex.withLock {
        val entry = backing[key]
        if (entry == null) return null
        // If types mismatch, we still return value as string for backward compatibility, but you can enforce type here.
        entry
    }

    private fun persistSync() {
        try {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            val serialized = json.encodeToString(mapSerializer, backing)
            tmp.writeText(serialized)
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: Exception) {
            // best-effort: ignore or log
        }
    }

    companion object Companion {
        private const val ALL_KEYS = "__ALL_KEYS__"

        /**
         * Convenience factory that creates DesktopSettings in the platform's application data directory.
         * Example path: ~/.config/<appId>/settings.json
         */
        fun createDefault(appId: String, fileName: String = "settings.json"): Settings {
            val home = System.getProperty("user.home") ?: "."
            val configDir = File(home, ".config/$appId")
            val file = File(configDir, fileName)
            return Settings(file)
        }
    }
}

// ----------------------------
// Optional: small Kotlin-friendly extension helpers for use from Compose
// ----------------------------

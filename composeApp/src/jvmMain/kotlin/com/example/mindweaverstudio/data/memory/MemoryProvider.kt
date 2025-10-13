package com.example.mindweaverstudio.data.memory

import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.LocalFileMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.Aes256GCMEncryptor
import ai.koog.agents.memory.storage.EncryptedStorage
import ai.koog.rag.base.files.JVMFileSystemProvider
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import java.nio.file.Path
import java.nio.file.Paths

class MemoryProvider(
    private val configuration: ApiConfiguration
) {

    private val secureStorage = EncryptedStorage(
        fs = JVMFileSystemProvider.ReadWrite,
        encryption = Aes256GCMEncryptor("my-secret-key")
    )
    private val appDataDir: Path = Paths.get(System.getProperty("user.home"))
        .resolve(".mindweaver")
        .resolve("memory")
        .also { it.toFile().mkdirs() }

    private val provider = LocalFileMemoryProvider(
        config = LocalMemoryConfig("mind-weaver-studio"),
        storage = secureStorage,
        fs = JVMFileSystemProvider.ReadWrite,
        root = appDataDir
    )

    fun getProvider(): AgentMemoryProvider {
        return provider
    }
}
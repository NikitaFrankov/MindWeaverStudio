package com.example.mindweaverstudio.ai.memory

import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.LocalFileMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.Aes256GCMEncryptor
import ai.koog.agents.memory.storage.Aes256GCMEncryptor.Companion.generateRandomKey
import ai.koog.agents.memory.storage.Aes256GCMEncryptor.Companion.keyToString
import ai.koog.agents.memory.storage.EncryptedStorage
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlin.io.path.Path

const val SECURE_AGENT_MEMORY = "secure_agent_memory"

class SecureAgentMemoryProvider(
    private val memoryProvider: AgentMemoryProvider = LocalFileMemoryProvider(
        config = LocalMemoryConfig("secure-mind-weaver-studio"),
        storage = EncryptedStorage(
            fs = JVMFileSystemProvider.ReadWrite,
            encryption = Aes256GCMEncryptor(secretKey = keyToString(generateRandomKey()))
        ),
        fs = JVMFileSystemProvider.ReadWrite,
        root = Path("")
    )
) : AgentMemoryProvider by memoryProvider
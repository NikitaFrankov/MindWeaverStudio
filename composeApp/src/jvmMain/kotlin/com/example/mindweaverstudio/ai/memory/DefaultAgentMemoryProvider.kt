package com.example.mindweaverstudio.ai.memory

import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.LocalFileMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.SimpleStorage
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlin.io.path.Path

const val DEFAULT_AGENT_MEMORY = "default_agent_memory"

class DefaultAgentMemoryProvider(
    private val memoryProvider: AgentMemoryProvider = LocalFileMemoryProvider(
        config = LocalMemoryConfig("mind-weaver-studio"),
        storage = SimpleStorage(JVMFileSystemProvider.ReadWrite),
        fs = JVMFileSystemProvider.ReadWrite,
        root = Path("")
    )
) : AgentMemoryProvider by memoryProvider
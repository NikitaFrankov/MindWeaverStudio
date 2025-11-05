package com.example.mindweaverstudio.ai.models

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

object LocalModels {

    object QWEN {

        val QWEN_3_8B: LLModel = LLModel(
            provider = LLMProvider.Ollama,
            id = "qwen3:8b",
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Schema.JSON.Basic,
                LLMCapability.Tools,
                LLMCapability.ToolChoice,
                LLMCapability.Completion,
            ),
            contextLength = 128_000,
        )
    }
}
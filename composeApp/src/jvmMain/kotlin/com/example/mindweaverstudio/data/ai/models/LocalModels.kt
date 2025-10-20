package com.example.mindweaverstudio.data.ai.models

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

object LocalModels {

    object QWEN {

        val QWEN_2_5_CODER_7B: LLModel = LLModel(
            provider = LLMProvider.Ollama,
            id = "qwen2.5-coder:7b",
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Schema.JSON.Basic,
                LLMCapability.Tools,
                LLMCapability.ToolChoice,
                LLMCapability.Completion,
            ),
            contextLength = 128_000,
        )

        val LLAMA_3_2_8B: LLModel = LLModel(
            provider = LLMProvider.Ollama,
            id = "llama3.1:8b",
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Schema.JSON.Basic,
                LLMCapability.Tools
            ),
            contextLength = 131_072,
        )
    }
}
package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.ai.memory.DEFAULT_AGENT_MEMORY
import com.example.mindweaverstudio.ai.pipelines.architecture.ArchitecturePipeline
import com.example.mindweaverstudio.ai.pipelines.chat.ChatPipeline
import com.example.mindweaverstudio.ai.pipelines.codeCreator.CodeCreatorPipeline
import com.example.mindweaverstudio.ai.pipelines.codeFix.CodeFixPipeline
import com.example.mindweaverstudio.ai.pipelines.githubRelease.GithubReleasePipeline
import org.koin.core.qualifier.named
import org.koin.dsl.module

val pipelinesModule = module {

    factory<ArchitecturePipeline> {
        ArchitecturePipeline(
            config = get(),
        )
    }

    factory<ChatPipeline> {
        ChatPipeline(
            config = get(),
        )
    }

    factory<CodeCreatorPipeline> {
        CodeCreatorPipeline(
            config = get(),
            codeCheckTools = get(),
        )
    }

    factory<CodeFixPipeline> {
        CodeFixPipeline(
            config = get(),
        )
    }

    factory<GithubReleasePipeline> {
        GithubReleasePipeline(
            githubTools = get(),
            configuration = get(),
            userTools = get(),
            memoryProvider = get(qualifier = named(DEFAULT_AGENT_MEMORY)),
        )
    }
}
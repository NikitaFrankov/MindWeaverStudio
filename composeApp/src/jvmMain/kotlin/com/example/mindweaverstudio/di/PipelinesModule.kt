package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.data.ai.pipelines.architecture.ArchitecturePipeline
import com.example.mindweaverstudio.data.ai.pipelines.chat.ChatPipeline
import com.example.mindweaverstudio.data.ai.pipelines.codeCreator.CodeCreatorPipeline
import com.example.mindweaverstudio.data.ai.pipelines.codeFix.CodeFixPipeline
import com.example.mindweaverstudio.data.ai.pipelines.githubRelease.GithubReleasePipeline
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
            tools = get(),
        )
    }
}
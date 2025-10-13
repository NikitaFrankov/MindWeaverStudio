package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.data.ai.tools.github.GithubTools
import com.example.mindweaverstudio.data.ai.tools.pipelines.CodePipelineTools
import org.koin.dsl.module

val toolsModule = module {

    factory<GithubTools> {
        GithubTools(
            githubClient = get()
        )
    }

    factory<CodePipelineTools> {
        CodePipelineTools(
            githubReleasePipeline = get(),
            architecturePipeline = get(),
            codeCreatorPipeline = get(),
            codeFixPipeline = get(),
            chatPipeline = get(),
            settings = get(),
        )
    }
}
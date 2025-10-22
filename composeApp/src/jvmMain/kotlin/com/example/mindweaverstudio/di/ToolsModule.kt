package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.ai.tools.codeCheck.CodeCheckTools
import com.example.mindweaverstudio.ai.tools.github.GithubTools
import com.example.mindweaverstudio.ai.tools.pipelines.CodePipelineTools
import org.koin.dsl.module

val toolsModule = module {

    factory<GithubTools> {
        GithubTools(
            githubClient = get()
        )
    }

    factory<CodeCheckTools> {
        CodeCheckTools(
            dockerClient = get(),
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
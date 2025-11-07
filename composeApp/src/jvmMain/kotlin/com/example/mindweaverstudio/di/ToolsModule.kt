package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.ai.tools.codeCheck.CodeCheckTools
import com.example.mindweaverstudio.ai.tools.github.GithubTools
import com.example.mindweaverstudio.ai.tools.pipelines.CodePipelineTools
import com.example.mindweaverstudio.ai.tools.user.UserInteractionTools
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

    factory<UserInteractionTools> {
        UserInteractionTools(systemInterruptionsProvider = get())
    }

    factory<CodePipelineTools> {
        CodePipelineTools(
            githubReleasePipeline = get(),
            architecturePipeline = get(),
            codeCreatorPipeline = get(),
            bugTriagePipeline = get(),
            codeFixPipeline = get(),
            chatPipeline = get(),
            settings = get(),
        )
    }
}
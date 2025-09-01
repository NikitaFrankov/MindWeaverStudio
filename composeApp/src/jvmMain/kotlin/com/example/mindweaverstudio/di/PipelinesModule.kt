package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.data.ai.agents.Agent
import com.example.mindweaverstudio.data.ai.agents.AgentsRegistry
import com.example.mindweaverstudio.data.ai.pipelines.CHAT_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.CODE_CREATOR_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.CODE_FIX_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.GITHUB_RELEASE_PIPELINE
import com.example.mindweaverstudio.data.ai.pipelines.Pipeline
import com.example.mindweaverstudio.data.ai.pipelines.PipelineFactory
import com.example.mindweaverstudio.data.ai.pipelines.flows.ChatPipeline
import com.example.mindweaverstudio.data.ai.pipelines.flows.CodeCreatorPipeline
import com.example.mindweaverstudio.data.ai.pipelines.flows.CodeFixPipeline
import com.example.mindweaverstudio.data.ai.pipelines.flows.GithubReleasePipeline
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val pipelinesModule = module {
    singleOf(::PipelineFactory)

    factory<Pipeline>(named(CHAT_PIPELINE)) {
        val agentNames = get<PipelineFactory>().chatPipelineAgents
        val registry = AgentsRegistry().apply {
            agentNames.forEach { agentName ->
                register(agentName, get<Agent>(named(agentName)))
            }
        }
        ChatPipeline(agentsRegistry = registry)
    }

    factory<Pipeline>(named(CODE_FIX_PIPELINE)) {
        val agentNames = get<PipelineFactory>().codeFixerPipelineAgents
        val registry = AgentsRegistry().apply {
            agentNames.forEach { agentName ->
                register(agentName, get<Agent>(named(agentName)))
            }
        }
        CodeFixPipeline(agentsRegistry = registry)
    }

    factory<Pipeline>(named(CODE_CREATOR_PIPELINE)) {
        val agentNames = get<PipelineFactory>().codeCreatorPipelineAgents
        val registry = AgentsRegistry().apply {
            agentNames.forEach { agentName ->
                register(agentName, get<Agent>(named(agentName)))
            }
        }
        CodeCreatorPipeline(agentsRegistry = registry)
    }

    factory<Pipeline>(named(GITHUB_RELEASE_PIPELINE)) {
        val agentNames = get<PipelineFactory>().githubReleasePipelineAgents
        val registry = AgentsRegistry().apply {
            agentNames.forEach { agentName ->
                register(agentName, get<Agent>(named(agentName)))
            }
        }
        GithubReleasePipeline(agentsRegistry = registry)
    }
}
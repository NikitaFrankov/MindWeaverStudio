package com.example.mindweaverstudio.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.ai.memory.DEFAULT_AGENT_MEMORY
import com.example.mindweaverstudio.ai.memory.DefaultAgentMemoryProvider
import com.example.mindweaverstudio.ai.memory.SECURE_AGENT_MEMORY
import com.example.mindweaverstudio.ai.memory.SecureAgentMemoryProvider
import com.example.mindweaverstudio.components.authentication.AuthenticationStoreFactory
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStoreFactory
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionStoreFactory
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputStoreFactory
import com.example.mindweaverstudio.components.userconfiguration.UserConfigurationStoreFactory
import com.example.mindweaverstudio.data.utils.config.ApiConfiguration
import com.example.mindweaverstudio.ai.orchestrator.code.CodeOrchestrator
import com.example.mindweaverstudio.data.auth.AuthManager
import com.example.mindweaverstudio.data.interruptions.InterruptionsBus
import com.example.mindweaverstudio.data.interruptions.SystemInterruptionsProvider
import com.example.mindweaverstudio.data.limits.LimitManager
import com.example.mindweaverstudio.data.receivers.CodeEditorLogReceiver
import com.example.mindweaverstudio.data.settings.Settings
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {

    single<Settings> { Settings.createDefault("com.example.mindweaverstudio") }
    singleOf(::AuthManager)
    singleOf(::LimitManager)

    // Configuration
    singleOf(ApiConfiguration::load) bind ApiConfiguration::class

    // Receivers
    singleOf(::CodeEditorLogReceiver)

    // Memory provider
    single(qualifier = named(DEFAULT_AGENT_MEMORY)) {
        DefaultAgentMemoryProvider()
    }
    single(qualifier = named(SECURE_AGENT_MEMORY)) {
        SecureAgentMemoryProvider()
    }

    // Clients
    includes(clientsModule)

    // Tools
    includes(toolsModule)

    // Pipelines
    includes(pipelinesModule)

    //Orchestrator
    factory<CodeOrchestrator> {
        CodeOrchestrator(
            tools = get(),
            configuration = get(),
        )
    }

    // Interruptions
    singleOf(::InterruptionsBus)
    singleOf(::SystemInterruptionsProvider)

    // Stores
    singleOf(::DefaultStoreFactory) bind StoreFactory::class
    factoryOf(::AuthenticationStoreFactory)
    factoryOf(::ProjectSelectionStoreFactory)
    factoryOf(::CodeEditorStoreFactory)
    factoryOf(::UserConfigurationStoreFactory)
    factoryOf(::RepoInfoInputStoreFactory)
}
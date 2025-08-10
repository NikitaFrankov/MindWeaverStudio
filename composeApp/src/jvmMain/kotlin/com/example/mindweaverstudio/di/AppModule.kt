package com.example.mindweaverstudio.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.mindweaverstudio.components.chat.ChatStoreFactory
import com.example.mindweaverstudio.config.ApiConfiguration
import com.example.mindweaverstudio.data.network.ChatGPTApiClient
import com.example.mindweaverstudio.data.network.DeepSeekApiClient
import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository
import com.example.mindweaverstudio.data.repository.chatgpt.ChatGPTRepositoryImpl
import com.example.mindweaverstudio.data.repository.deepseek.DeepSeekRepositoryImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    // Configuration
    single { ApiConfiguration.load() }
    
    // Core
    single<StoreFactory> { DefaultStoreFactory() }
    
    // Network Clients
    single(named("deepseek")) { DeepSeekApiClient(get<ApiConfiguration>().deepSeekApiKey) }
    single(named("chatgpt")) { ChatGPTApiClient(get<ApiConfiguration>().openAiApiKey) }
    
    // Repositories
    single<NeuralNetworkRepository>(named("deepseek")) { DeepSeekRepositoryImpl(get(named("deepseek"))) }
    single<NeuralNetworkRepository>(named("chatgpt")) { ChatGPTRepositoryImpl(get(named("chatgpt"))) }
    
    // Default repository (can be configured)
    single<NeuralNetworkRepository> { get<NeuralNetworkRepository>(named("deepseek")) }
    
    // Store Factory
    single { ChatStoreFactory(get(), get()) }
}
package com.example.mindweaverstudio.services

import com.example.mindweaverstudio.data.repository.NeuralNetworkRepository

interface RepositoryProvider {
    fun getRepository(providerName: String): NeuralNetworkRepository
}

class DefaultRepositoryProvider(
    private val deepSeekRepository: NeuralNetworkRepository,
    private val chatGPTRepository: NeuralNetworkRepository,
    private val geminiRepository: NeuralNetworkRepository
) : RepositoryProvider {
    
    override fun getRepository(providerName: String): NeuralNetworkRepository {
        return when (providerName) {
            "DeepSeek" -> deepSeekRepository
            "ChatGPT" -> chatGPTRepository
            "Gemini" -> geminiRepository
            else -> deepSeekRepository
        }
    }
}
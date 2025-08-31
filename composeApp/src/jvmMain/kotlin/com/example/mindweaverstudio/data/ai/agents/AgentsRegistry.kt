package com.example.mindweaverstudio.data.ai.agents

class AgentsRegistry {
    private val agents = mutableMapOf<String, Agent>()

    fun register(name: String, agent: Agent) {
        agents[name] = agent
    }

    fun get(name: String): Agent? = agents[name]
}
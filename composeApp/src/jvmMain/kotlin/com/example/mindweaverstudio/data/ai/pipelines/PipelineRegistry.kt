package com.example.mindweaverstudio.data.ai.pipelines

class PipelineRegistry {
    private val agents = mutableMapOf<String, Pipeline>()

    fun register(name: String, agent: Pipeline) {
        agents[name] = agent
    }

    fun get(name: String): Pipeline? = agents[name]

    fun getPresentableList(): List<String> {
        return agents.map { (name, pipeline) ->
            "Name: $name, description: ${pipeline.description}"
        }
    }
}
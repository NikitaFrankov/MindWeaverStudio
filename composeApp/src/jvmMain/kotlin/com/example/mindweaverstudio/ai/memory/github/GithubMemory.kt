package com.example.mindweaverstudio.ai.memory.github

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope

val githubOwnerConcept =
    Concept(keyword = "repo-owner", description = "Repository owner", factType = FactType.SINGLE)
val githubRepoConcept =
    Concept(keyword = "repo-name", description = "Repository name", factType = FactType.SINGLE)

val githubAgentScope = MemoryScope.Agent("github-pipeline-agent")

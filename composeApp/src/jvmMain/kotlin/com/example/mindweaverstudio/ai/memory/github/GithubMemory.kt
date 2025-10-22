package com.example.mindweaverstudio.ai.memory.github

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope

val githubOwnerConcept = Concept("repo-owner", "Repository owner", FactType.MULTIPLE)
val githubRepoConcept = Concept("repo-name", "Repository name", FactType.MULTIPLE)

val githubAgentScope = MemoryScope.Agent("github-pipeline-agent")

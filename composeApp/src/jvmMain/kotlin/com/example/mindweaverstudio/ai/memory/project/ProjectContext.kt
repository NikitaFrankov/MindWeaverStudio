package com.example.mindweaverstudio.ai.memory.project

import ai.koog.agents.memory.model.MemorySubject
import kotlinx.serialization.Serializable

@Serializable
object ProjectContext : MemorySubject() {
    override val name = "project"
    override val promptDescription = "Active development project context"
    override val priorityLevel = 2
}
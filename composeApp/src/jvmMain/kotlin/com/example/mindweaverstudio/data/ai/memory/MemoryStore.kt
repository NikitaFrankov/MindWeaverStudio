package com.example.mindweaverstudio.data.ai.memory
import com.example.mindweaverstudio.data.models.pipeline.PipelineResult
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Duration

@Serializable
data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AgentSnapshot(
    val checkpointId: String,
    val conversationWindow: List<MemoryItem> = emptyList(),
    val pendingTasks: List<String> = emptyList(),
    val variables: Map<String, String> = emptyMap(),
    val agentVersion: String = "v1",
    val createdAt: Long = System.currentTimeMillis()
)

interface MemoryStore {
    suspend fun saveMemory(item: MemoryItem, ttl: Duration? = null)
    suspend fun getMemory(id: String): MemoryItem?
    suspend fun deleteMemory(id: String)
    suspend fun queryRecent(limit: Int = 50): List<MemoryItem>

    suspend fun saveCheckpoint(id: String, snapshot: AgentSnapshot)
    suspend fun loadCheckpoint(id: String): AgentSnapshot?
    suspend fun listCheckpoints(prefix: String = ""): List<String>

    suspend fun acquireLock(lockKey: String, ttl: Duration, ownerId: String = UUID.randomUUID().toString()): Boolean
    suspend fun releaseLock(lockKey: String, ownerId: String): Boolean

    suspend fun close()
}

suspend fun MemoryStore.getLastCompletedStep(pipelineName: String): Int? {
    return queryRecent(limit = 1)
        .firstOrNull { it.metadata["pipeline"] == pipelineName && it.metadata["status"] == "success" }
        ?.metadata?.get("step")?.toInt()
}

suspend fun MemoryStore.getStepOutput(pipelineName: String, stepId: Int): String? {
    return queryRecent(limit = 10)
        .find { it.metadata["pipeline"] == pipelineName && it.metadata["step"] == stepId.toString() }
        ?.content
}

suspend fun MemoryStore.saveStepResult(
    pipelineName: String,
    stepId: Int,
    agentName: String,
    result: PipelineResult
) {
    saveMemory(
        MemoryItem(
            id = UUID.randomUUID().toString(),
            type = "pipeline_snapshot",
            content = result.message,
            metadata = mapOf(
                "pipeline" to pipelineName,
                "step" to stepId.toString(),
                "agent" to agentName,
                "status" to if (result.isError) "error" else "success"
            ),
            createdAt = System.currentTimeMillis()
        )
    )
}

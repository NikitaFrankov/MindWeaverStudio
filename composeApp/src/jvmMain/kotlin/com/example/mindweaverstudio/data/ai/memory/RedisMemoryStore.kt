package com.example.mindweaverstudio.data.ai.memory


import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import redis.clients.jedis.Jedis
import redis.clients.jedis.Pipeline
import redis.clients.jedis.params.SetParams
import kotlinx.serialization.json.Json
import redis.clients.jedis.params.ScanParams
import redis.clients.jedis.resps.ScanResult
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Production-ready RedisMemoryStore using Jedis.
 *
 * Keyspace layout:
 * - memory:{id} : Hash with memory fields
 * - memories:timeline : Sorted Set (createdAt as score)
 * - checkpoint:{id} : Hash with checkpoint snapshot
 * - lock:{id} : String (for distributed lock)
 */
class RedisMemoryStore(
    private val jedis: Jedis,
) : MemoryStore {

    private val keyPrefix: String = ""
    private val json = Json { encodeDefaults = true; prettyPrint = false }

    private val timelineKey = prefix("memories:timeline")
    private val checkpointPrefix = prefix("checkpoint")
    private val memoryPrefix = prefix("memory")

    private fun prefix(k: String): String =
        if (keyPrefix.isBlank()) k else "$keyPrefix:$k"

    private fun memoryKey(id: String) = "$memoryPrefix:$id"
    private fun checkpointKey(id: String) = "$checkpointPrefix:$id"

    override suspend fun saveMemory(item: MemoryItem, ttl: Duration?) = withContext(Dispatchers.IO) {
        jedis.use { j ->
            val p: Pipeline = j.pipelined()
            val map = mapOf(
                "id" to item.id,
                "type" to item.type,
                "content" to item.content,
                "metadata" to json.encodeToString(item.metadata),
                "createdAt" to item.createdAt.toString()
            )
            p.hmset(memoryKey(item.id), map)
            p.zadd(timelineKey, item.createdAt.toDouble(), item.id)
            if (ttl != null) {
                val seconds = ttl.toDouble(DurationUnit.SECONDS).toLong()
                if (seconds > 0) p.expire(memoryKey(item.id), seconds)
            }
            p.sync()
        }
    }

    override suspend fun getMemory(id: String): MemoryItem? = withContext(Dispatchers.IO) {
        val map = jedis.hgetAll(memoryKey(id))
        if (map == null || map.isEmpty()) return@withContext null
        MemoryItem(
            id = map["id"] ?: id,
            type = map["type"] ?: "unknown",
            content = map["content"] ?: "",
            metadata = map["metadata"]?.let { json.decodeFromString(it) } ?: emptyMap(),
            createdAt = map["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis()
        )
    }

    override suspend fun deleteMemory(id: String) = withContext(Dispatchers.IO) {
        jedis.use { j ->
            val p = j.pipelined()
            p.del(memoryKey(id))
            p.zrem(timelineKey, id)
            p.sync()
        }
    }

    override suspend fun queryRecent(limit: Int): List<MemoryItem> = withContext(Dispatchers.IO) {
        val ids = jedis.zrevrange(timelineKey, 0, (limit - 1).toLong())
        if (ids.isEmpty()) return@withContext emptyList()

        jedis.use { j ->
            val p = j.pipelined()
            val responses = ids.map { id -> p.hgetAll(memoryKey(id)) }
            p.sync()

            ids.mapIndexedNotNull { i, id ->
                val map = responses[i].get()
                if (map != null && map.isNotEmpty()) {
                    MemoryItem(
                        id = map["id"] ?: id,
                        type = map["type"] ?: "unknown",
                        content = map["content"] ?: "",
                        metadata = map["metadata"]?.let { json.decodeFromString(it) } ?: emptyMap(),
                        createdAt = map["createdAt"]?.toLongOrNull() ?: 0L
                    )
                } else null
            }
        }
    }

    override suspend fun saveCheckpoint(id: String, snapshot: AgentSnapshot): Unit = withContext(Dispatchers.IO) {
        val key = checkpointKey(id)
        val map = mapOf(
            "snapshot" to json.encodeToString(snapshot),
            "createdAt" to snapshot.createdAt.toString(),
            "agentVersion" to snapshot.agentVersion
        )
        jedis.hmset(key, map)
    }

    override suspend fun loadCheckpoint(id: String): AgentSnapshot? = withContext(Dispatchers.IO) {
        val key = checkpointKey(id)
        val map = jedis.hgetAll(key)
        if (map == null || map.isEmpty()) return@withContext null
        map["snapshot"]?.let { json.decodeFromString<AgentSnapshot>(it) }
    }

    override suspend fun listCheckpoints(prefix: String): List<String> = withContext(Dispatchers.IO) {
        val keyPattern = if (prefix.isBlank()) "$checkpointPrefix:*" else "$checkpointPrefix:$prefix*"
        val results = mutableListOf<String>()
        var cursor = "0"
        val params = ScanParams().match(keyPattern).count(100)
        do {
            val scanResult: ScanResult<String> = jedis.scan(cursor, params)
            cursor = scanResult.cursor
            for (k in scanResult.result) {
                results += k.removePrefix("$checkpointPrefix:")
            }
        } while (cursor != "0")
        results
    }

    override suspend fun acquireLock(lockKey: String, ttl: Duration, ownerId: String): Boolean = withContext(Dispatchers.IO) {
        val fullKey = prefix("lock:$lockKey")
        val setParams = SetParams.setParams().nx().px(ttl.toDouble(DurationUnit.MILLISECONDS).toLong())
        jedis.set(fullKey, ownerId, setParams) == "OK"
    }

    override suspend fun releaseLock(lockKey: String, ownerId: String): Boolean = withContext(Dispatchers.IO) {
        val fullKey = prefix("lock:$lockKey")
        val lua = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
        """.trimIndent()
        val r = jedis.eval(lua, listOf(fullKey), listOf(ownerId))
        (r as Long) == 1L
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        jedis.close()
    }
}
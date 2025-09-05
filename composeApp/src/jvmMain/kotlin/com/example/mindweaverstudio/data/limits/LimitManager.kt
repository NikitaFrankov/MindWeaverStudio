package com.example.mindweaverstudio.data.limits

import com.example.mindweaverstudio.data.models.ai.Role

class LimitManager {
    private val usage = mutableMapOf<String, MutableMap<String, Pair<Int, Long>>>()
    private val defaultLimits = mapOf(
        Role.ADMIN to mapOf("daily_queries" to Int.MAX_VALUE),
        Role.USER to mapOf("daily_queries" to 100),
        Role.GUEST to mapOf("daily_queries" to 10)
    )

    fun checkAndConsume(username: String, limitKey: String, role: Role, consumption: Int = 1): Boolean {
        val maxLimit = defaultLimits[role]?.get(limitKey) ?: 0
        val userUsage = usage.getOrPut(username) { mutableMapOf() }
        val now = System.currentTimeMillis()
        var (used, resetAt) = userUsage.getOrPut(limitKey) { 0 to (now + 86_400_000) }

        if (now > resetAt) {
            used = 0
            resetAt = now + 86_400_000
        }

        if (used + consumption > maxLimit) return false

        used += consumption
        userUsage[limitKey] = used to resetAt
        return true
    }
}
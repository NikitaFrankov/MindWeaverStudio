package com.example.mindweaverstudio.data.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.example.mindweaverstudio.data.settings.Settings
import java.util.Date

private const val TOKEN_KEY = "auth_token"

class AuthManager(
    private val settings: Settings
) {
    private val secret = "your-secure-secret-key"
    private val algorithm = Algorithm.HMAC256(secret)
    private val verifier: JWTVerifier = JWT.require(algorithm).build()

    private val users = mutableMapOf(
        "admin" to ("adminpass" to "ADMIN"),
        "user" to ("userpass" to "USER")
    )

    suspend fun generateToken(username: String, password: String): String? {
        val userData = users[username]
        if (userData != null && password == userData.first) {
            val role = userData.second
            return JWT.create()
                .withSubject(username)
                .withClaim("role", role)
                .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000))
                .sign(algorithm)
                .also { saveToken(it) }
        }
        return null
    }

    fun validateToken(token: String): Map<String, Any>? {
        return try {
            val decoded = verifier.verify(token)
            mapOf(
                "username" to decoded.subject,
                "role" to decoded.getClaim("role").asString()
            )
        } catch (e: JWTVerificationException) {
            null
        }
    }

    suspend fun saveToken(token: String) { settings.putString(TOKEN_KEY, token) }
    suspend fun getToken(): String? = settings.getString(TOKEN_KEY, "")
    suspend fun clearToken() { settings.remove(TOKEN_KEY) }

    // Добавь пользователя (для динамики)
    fun registerUser(username: String, password: String, role: String) {
        users[username] = password to role
    }
}
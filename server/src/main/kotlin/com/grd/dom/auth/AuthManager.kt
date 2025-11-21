package com.grd.dom.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class InvalidCredentialsException(message: String) : Exception(message)

private data class ServerUserAccount(
    val username: String,
    val password: String
)

class JwtTokenProvider(
    secret: String,
    private val issuer: String = "dom-auth",
    private val audience: String = "dom-clients",
    private val expirationMinutes: Long = 60
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun issueToken(username: String, index: Int): String {
        val now = Instant.now()
        val expires = now.plus(expirationMinutes, ChronoUnit.MINUTES)
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("username", username)
            .withClaim("tokenIndex", index)
            .withClaim("tokenLabel", "Token-${index + 1}")
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expires))
            .sign(algorithm)
    }
}

class DefaultAuthService(
    private val tokenProvider: JwtTokenProvider,
    private val tokenCount: Int = DEFAULT_TOKEN_COUNT,
    private val defaultPassword: String = DEFAULT_USER_PASSWORD
) {
    private val accounts = listOf("user1", "user2", "user3")
        .map { normalized -> ServerUserAccount(username = normalized, password = defaultPassword) }
        .associateBy { it.username }

    fun authenticate(username: String, password: String): AuthResult {
        val normalized = normalize(username)
        val account = accounts[normalized] ?: throw InvalidCredentialsException("Unknown user \"$username\"")
        if (account.password != password) {
            throw InvalidCredentialsException("Incorrect password for \"$username\"")
        }
        val tokens = (0 until tokenCount).map { index ->
            tokenProvider.issueToken(account.username, index)
        }
        return AuthResult(username = account.username, tokens = tokens)
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(" ", "")
}

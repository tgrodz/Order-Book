package com.grd.dom.auth

import com.grd.dom.SERVER_PORT
import kotlinx.serialization.Serializable

const val DEFAULT_AUTH_HOST = "10.0.2.2"
const val DEFAULT_AUTH_BASE_URL = "http://$DEFAULT_AUTH_HOST:$SERVER_PORT"
const val DEFAULT_USER_PASSWORD = "123456"
const val DEFAULT_TOKEN_COUNT = 9

@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResult(
    val username: String,
    val tokens: List<String>
)

@Serializable
data class AuthErrorResponse(
    val message: String
)

data class AuthSession(
    val username: String,
    val token: String
)

class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

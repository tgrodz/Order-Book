package com.grd.dom.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AuthApi(
    private val baseUrl: String = DEFAULT_AUTH_BASE_URL,
    httpClient: HttpClient? = null,
    private val serializer: Json = Json { ignoreUnknownKeys = true }
) {
    private val ownsClient = httpClient == null
    private val client = httpClient ?: defaultHttpClient(serializer)

    suspend fun authenticate(username: String, password: String): AuthResult {
        val response = client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username = username, password = password))
        }
        return response.body()
    }

    fun close() {
        if (ownsClient) {
            client.close()
        }
    }

    companion object {
        private fun defaultHttpClient(serializer: Json) = HttpClient {
            install(ContentNegotiation) { json(serializer) }
        }
    }
}

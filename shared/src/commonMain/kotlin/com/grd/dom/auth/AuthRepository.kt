package com.grd.dom.auth

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException

class AuthRepository(
    private val api: AuthApi = AuthApi()
) {
    suspend fun authenticate(username: String, password: String): AuthResult =
        try {
            api.authenticate(username = username, password = password)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: ClientRequestException) {
            val message = extractErrorMessage(ex.response) ?: "Invalid username or password"
            throw AuthException(message, ex)
        } catch (ex: ServerResponseException) {
            throw AuthException("Server error: ${ex.response.status.value}", ex)
        } catch (ex: Exception) {
            throw AuthException(ex.message ?: "Unable to authenticate", ex)
        }

    private suspend fun extractErrorMessage(response: HttpResponse): String? =
        runCatching { response.body<AuthErrorResponse>().message }.getOrNull()

    fun close() = api.close()
}

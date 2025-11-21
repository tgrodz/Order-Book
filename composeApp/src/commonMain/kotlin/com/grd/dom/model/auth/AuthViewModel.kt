package com.grd.dom.model.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.grd.dom.auth.AuthException
import com.grd.dom.auth.AuthRepository
import com.grd.dom.auth.AuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val scope: CoroutineScope = MainScope()
) {
    var uiState by mutableStateOf(AuthUiState())
        private set

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(username = value)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value)
    }

    fun login(onAuthenticated: (AuthSession) -> Unit) {
        if (uiState.isLoading) return
        scope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                val result = authRepository.authenticate(
                    uiState.username.trim(),
                    uiState.password
                )
                val token = result.tokens.firstOrNull()
                    ?: throw AuthException("Server did not return a login token.")
                onAuthenticated(AuthSession(username = result.username, token = token))
            } catch (ex: AuthException) {
                uiState = uiState.copy(
                    errorMessage = ex.message ?: "Unable to authenticate"
                )
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun clear() {
        authRepository.close()
        scope.cancel()
    }
}

package com.grd.dom.model.auth

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotBlank()
}

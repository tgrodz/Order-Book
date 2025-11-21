package com.grd.dom.controllers

import com.grd.dom.auth.AuthErrorResponse
import com.grd.dom.auth.AuthRequest
import com.grd.dom.auth.DefaultAuthService
import com.grd.dom.auth.InvalidCredentialsException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class AuthController(
    private val authService: DefaultAuthService
) {
    fun install(route: Route) {
        route.route("/auth") {
            post("/login") { login(call) }
        }
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receive<AuthRequest>()
        try {
            val result = authService.authenticate(
                username = request.username,
                password = request.password
            )
            call.respond(result)
        } catch (ex: InvalidCredentialsException) {
            val error = AuthErrorResponse(message = ex.message ?: "Invalid credentials")
            call.respond(HttpStatusCode.Unauthorized, error)
        }
    }
}

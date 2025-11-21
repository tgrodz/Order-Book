package com.grd.dom.di

import com.grd.dom.auth.DefaultAuthService
import com.grd.dom.auth.JwtTokenProvider
import com.grd.dom.config.ServerConfig
import com.grd.dom.controllers.AuthController
import com.grd.dom.controllers.QuoteController
import com.grd.dom.quote.QuoteStreamHandler
import kotlinx.serialization.json.Json

data class ServerDependencies(
    val config: ServerConfig,
    val json: Json,
    val authService: DefaultAuthService,
    val quoteStreamHandler: QuoteStreamHandler,
    val authController: AuthController,
    val quoteController: QuoteController
)
// Process-level singletons
fun buildServerDependencies(): ServerDependencies {
    val config = ServerConfig.fromEnvironment()
    val json = Json { ignoreUnknownKeys = true }
    val jwtProvider = JwtTokenProvider(secret = config.jwtSecret)
    val authService = DefaultAuthService(tokenProvider = jwtProvider)
    val quoteStreamHandler = QuoteStreamHandler(json = json)
    val authController = AuthController(authService)
    val quoteController = QuoteController(quoteStreamHandler)
    return ServerDependencies(
        config = config,
        json = json,
        authService = authService,
        quoteStreamHandler = quoteStreamHandler,
        authController = authController,
        quoteController = quoteController
    )
}

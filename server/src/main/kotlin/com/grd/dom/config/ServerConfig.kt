package com.grd.dom.config

data class ServerConfig(
    val jwtSecret: String
) {
    companion object {
        fun fromEnvironment(): ServerConfig {
            val secret = System.getenv("DOM_JWT_SECRET") ?: "dom-default-jwt-secret"
            return ServerConfig(jwtSecret = secret)
        }
    }
}

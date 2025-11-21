package com.grd.dom.plugins

import com.grd.dom.SERVER_PORT
import com.grd.dom.di.ServerDependencies
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(dependencies: ServerDependencies) {
    routing {
        get("/") {
            call.respondText("DOM Quote Server running on port $SERVER_PORT")
        }
        dependencies.authController.install(this)
        dependencies.quoteController.install(this)
    }
}

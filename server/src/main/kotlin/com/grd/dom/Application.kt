package com.grd.dom

import com.grd.dom.di.buildServerDependencies
import com.grd.dom.plugins.configureRouting
import com.grd.dom.plugins.configureSerialization
import com.grd.dom.plugins.configureSockets
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val dependencies = buildServerDependencies()
    configureSockets()
    configureSerialization(dependencies.json)
    configureRouting(dependencies)
}

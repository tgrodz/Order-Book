package com.grd.dom.controllers

import com.grd.dom.quote.QuoteStreamHandler
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.server.websocket.DefaultWebSocketServerSession

class QuoteController(
    private val quoteStreamHandler: QuoteStreamHandler
) {
    fun install(route: Route) {
        route.webSocket("/quotes") {
            connect(this)
        }
    }

    suspend fun connect(session: DefaultWebSocketServerSession) {
        quoteStreamHandler.handle(session)
    }
}

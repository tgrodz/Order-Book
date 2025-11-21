package com.grd.dom.quote

import com.grd.dom.trading.AggressivenessMode
import com.grd.dom.trading.QuoteMessage
import com.grd.dom.trading.QuoteSimulator
import com.grd.dom.trading.RemoteCommand
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuoteStreamHandler(
    private val json: Json,
    private val simulatorFactory: () -> QuoteSimulator = { QuoteSimulator() }
) {
    suspend fun handle(session: DefaultWebSocketServerSession) {
        val simulator = simulatorFactory().also { it.startTrading() }
        val senderScope = CoroutineScope(Dispatchers.Default)
        val senderJob = senderScope.launch {
            while (isActive) {
                val quote = simulator.nextQuote()
                session.send(Frame.Text(json.encodeToString(QuoteMessage.serializer(), quote)))
                delay(simulator.nextDelayMillis())
            }
        }

        try {
            session.incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    runCatching {
                        json.decodeFromString(RemoteCommand.serializer(), frame.readText())
                    }.onSuccess { command ->
                        handleCommand(simulator, command)
                    }
                }
            }
        } finally {
            senderJob.cancelAndJoin()
            senderScope.cancel()
            session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Client disconnected"))
        }
    }

    private fun handleCommand(simulator: QuoteSimulator, command: RemoteCommand) {
        when (command.command) {
            "setAggressiveness" -> command.mode?.let { simulator.setAggressiveness(AggressivenessMode.fromLevel(it)) }
            "startTrading" -> simulator.startTrading()
            "stopTrading" -> simulator.stopTrading()
        }
    }
}

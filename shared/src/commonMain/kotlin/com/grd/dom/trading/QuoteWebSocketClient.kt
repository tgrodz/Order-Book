package com.grd.dom.trading

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.send
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class QuoteWebSocketClient(
    private val endpoint: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)
    private val _updates = MutableSharedFlow<QuoteMessage>(replay = 1, extraBufferCapacity = 1)
    val updates: SharedFlow<QuoteMessage> = _updates.asSharedFlow()

    private val client = HttpClient {
        install(WebSockets)
        install(ContentNegotiation) { json(json) }
    }

    private var session: WebSocketSession? = null
    private var readerJob: Job? = null

    suspend fun connect() {
        disconnect()
        session = client.webSocketSession { url(endpoint) }
        val activeSession = session ?: return
        readerJob = scope.launch { pumpIncoming(activeSession) }
    }

    private suspend fun pumpIncoming(activeSession: WebSocketSession) {
        try {
            while (true) {
                val frame = try {
                    activeSession.incoming.receive()
                } catch (_: ClosedReceiveChannelException) {
                    break
                }
                if (frame is Frame.Text) {
                    handleFrame(frame)
                }
            }
        } catch (_: Throwable) {
            // Drop the connection silently; UI will update through flow completion.
        }
    }

    private suspend fun handleFrame(frame: Frame.Text) {
        runCatching {
            json.decodeFromString(QuoteMessage.serializer(), frame.readText())
        }.onSuccess { message ->
            _updates.emit(message)
        }
    }

    suspend fun sendCommand(command: RemoteCommand) {
        val activeSession = session ?: return
        val payload = json.encodeToString(RemoteCommand.serializer(), command)
        activeSession.send(Frame.Text(payload))
    }

    suspend fun disconnect() {
        readerJob?.cancelAndJoin()
        readerJob = null
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client closed"))
        session = null
    }

    suspend fun close() {
        disconnect()
        client.close()
        job.cancel()
    }
}

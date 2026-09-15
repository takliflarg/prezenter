package com.prezenter.app.network

import android.os.Build
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import java.util.concurrent.TimeUnit

enum class ConnectionState { DISCONNECTED, CONNECTING, PAIRING, CONNECTED, ERROR }

/**
 * Windows'dagi PrezenterServer bilan WebSocket orqali gaplashadi.
 * PresenterService shu klientni ushlab turadi va butun ilova
 * (Compose UI hamda tovush tugmasi eventlari) shu orqali buyruq yuboradi.
 */
class PresenterWebSocketClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state

    private val _events = MutableSharedFlow<InboundEnvelope>(extraBufferCapacity = 16)
    val events: SharedFlow<InboundEnvelope> = _events

    private var pendingPairing: PairingPayload? = null

    fun connect(payload: PairingPayload) {
        pendingPairing = payload
        _state.value = ConnectionState.CONNECTING

        val request = Request.Builder()
            .url("ws://${payload.ip}:${payload.port}/prezenter/")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _state.value = ConnectionState.PAIRING
                val deviceName = Build.MODEL ?: "Android qurilma"
                sendRaw(PairRequest(token = payload.token, deviceName = deviceName))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val envelope = runCatching { json.decodeFromString<InboundEnvelope>(text) }.getOrNull()
                    ?: return
                if (envelope.type == "pairResult") {
                    _state.value = if (envelope.success == true) ConnectionState.CONNECTED else ConnectionState.ERROR
                }
                _events.tryEmit(envelope)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.value = ConnectionState.ERROR
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = ConnectionState.DISCONNECTED
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "user disconnected")
        webSocket = null
        _state.value = ConnectionState.DISCONNECTED
    }

    fun sendCommand(action: String) {
        sendRaw(CommandRequest(action = action))
    }

    fun sendLaserToggle(on: Boolean) {
        sendRaw(LaserToggleRequest(action = if (on) LaserAction.ON else LaserAction.OFF))
    }

    fun sendLaserMove(x: Double, y: Double) {
        sendRaw(LaserMoveRequest(x = x, y = y))
    }

    private inline fun <reified T> sendRaw(message: T) {
        val text = json.encodeToString(message)
        webSocket?.send(text)
    }
}

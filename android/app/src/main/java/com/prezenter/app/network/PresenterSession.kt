package com.prezenter.app.network

/**
 * Butun ilova bo'ylab bitta WebSocket ulanishni ushlab turadigan
 * singleton. Compose UI (tugmalar) va PresenterService (tovush
 * tugmalari orqali) xuddi shu ulanishdan foydalanadi - shu bois
 * ekran yoniq/o'chiqligidan qat'iy nazar bitta izchil sessiya bo'ladi.
 */
object PresenterSession {
    val client = PresenterWebSocketClient()

    /** Oxirgi muvaffaqiyatli pairing payloadi - qayta ulanish uchun saqlanadi. */
    var lastPairing: PairingPayload? = null
        private set

    fun connect(payload: PairingPayload) {
        lastPairing = payload
        client.connect(payload)
    }

    fun reconnectIfPossible(): Boolean {
        val payload = lastPairing ?: return false
        client.connect(payload)
        return true
    }

    fun disconnect() {
        client.disconnect()
    }
}

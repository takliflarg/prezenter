package com.prezenter.app.network

import kotlinx.serialization.Serializable

/**
 * docs/PROTOCOL.md dagi JSON xabar konvertlarining Kotlin ko'rinishi.
 * Windows tomonidagi PrezenterServer.Models.InboundMessage bilan bir xil shaklda.
 */

object CommandAction {
    const val NEXT = "NEXT"
    const val PREV = "PREV"
    const val START_FROM_BEGINNING = "START_FROM_BEGINNING"
    const val START_FROM_CURRENT = "START_FROM_CURRENT"
    const val STOP = "STOP"
    const val VOLUME_UP = "VOLUME_UP"
    const val VOLUME_DOWN = "VOLUME_DOWN"
    const val MUTE_TOGGLE = "MUTE_TOGGLE"
}

object LaserAction {
    const val ON = "ON"
    const val OFF = "OFF"
    const val MOVE = "MOVE"
}

@Serializable
data class PairRequest(
    val type: String = "pair",
    val token: String? = null,
    val deviceName: String
)

@Serializable
data class CommandRequest(
    val type: String = "command",
    val action: String
)

@Serializable
data class LaserMoveRequest(
    val type: String = "laser",
    val action: String = LaserAction.MOVE,
    val x: Double,
    val y: Double
)

@Serializable
data class LaserToggleRequest(
    val type: String = "laser",
    val action: String
)

@Serializable
data class PingRequest(val type: String = "ping")

/** Serverdan kelgan xabarlarni "type" bo'yicha ajratib olish uchun minimal model. */
@Serializable
data class InboundEnvelope(
    val type: String,
    val success: Boolean? = null,
    val message: String? = null,
    val connected: Boolean? = null,
    val computerName: String? = null,
    val muted: Boolean? = null,
    val volume: Int? = null,
    /** Faqat tarmoqdan avtomatik topilib, PC'da yangi tasdiqlanganda keladi. */
    val token: String? = null
)

/**
 * Pairing payloadi. QR flow'da `token` QR koddan olinadi; tarmoqdan
 * avtomatik topilgan (hali tasdiqlanmagan) kompyuterga ulanishda
 * `token = null` - server bu holda PC foydalanuvchisidan tasdiq so'raydi.
 */
@Serializable
data class PairingPayload(
    val ip: String,
    val port: Int,
    val token: String? = null
)

/** NSD orqali topilgan kompyuter. */
data class DiscoveredComputer(
    val name: String,
    val host: String,
    val port: Int
)

package com.prezenter.app.pairing

import android.content.Context

/**
 * QR orqali bir marta tasdiqlangan kompyuterning pairing tokenini
 * "ip:port" kaliti bilan telefonda saqlaydi - shu bois keyinchalik
 * o'sha kompyuter Wi-Fi'da avtomatik topilganda (NSD) foydalanuvchi
 * QR kodni qayta skanerlamasdan ro'yxatdan tanlab ulanishi mumkin.
 */
class PairedDeviceStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getToken(hostPortKey: String): String? = prefs.getString(hostPortKey, null)

    fun saveToken(hostPortKey: String, token: String) {
        prefs.edit().putString(hostPortKey, token).apply()
    }

    companion object {
        private const val PREFS_NAME = "paired_devices"
    }
}

package com.prezenter.app.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bir xil Wi-Fi tarmog'idagi PrezenterServer'larni mDNS (NSD) orqali
 * avtomatik topadi, shunda foydalanuvchi har safar QR skanerlashi
 * shart bo'lmaydi - ro'yxatdan kompyuterni tanlab, saqlangan token
 * bilan ulanaveradi (token PairingManager tomonida eslab qolinadi).
 */
class DeviceDiscovery(context: Context) {

    private val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _computers = MutableStateFlow<List<DiscoveredComputer>>(emptyList())
    val computers: StateFlow<List<DiscoveredComputer>> = _computers

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (discoveryListener != null) return

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.startsWith(SERVICE_TYPE)) {
                    resolve(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _computers.value = _computers.value.filterNot { it.name == serviceInfo.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { nsdManager.stopServiceDiscovery(this) }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun resolve(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: return
                val computer = DiscoveredComputer(serviceInfo.serviceName, host, serviceInfo.port)
                _computers.value = _computers.value.filterNot { it.name == computer.name } + computer
            }
        })
    }

    fun stopDiscovery() {
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
        _computers.value = emptyList()
    }

    companion object {
        private const val SERVICE_TYPE = "_prezenter._tcp."
    }
}

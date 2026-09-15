package com.prezenter.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.prezenter.app.network.ConnectionState
import com.prezenter.app.network.DeviceDiscovery
import com.prezenter.app.network.PairingPayload
import com.prezenter.app.network.PresenterSession
import com.prezenter.app.pairing.PairedDeviceStore
import com.prezenter.app.pairing.QrScanActivity

@Composable
fun ConnectScreen(onConnected: () -> Unit) {
    val context = LocalContext.current
    val discovery = remember { DeviceDiscovery(context) }
    val store = remember { PairedDeviceStore(context) }
    val computers by discovery.computers.collectAsState()
    val connectionState by PresenterSession.client.state.collectAsState()
    val lastError by PresenterSession.client.lastError.collectAsState()

    // Hozir ulanishga urinilayotgan (yoki PC'da tasdiqlanishini kutayotgan)
    // qurilmaning "ip:port" kaliti - yangi token kelganda shu kalit bilan saqlanadi.
    var pendingKey by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        discovery.startDiscovery()
        onDispose { discovery.stopDiscovery() }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            onConnected()
        }
    }

    // Server tarmoqdan topilgan (tokensiz) qurilmani PC'da tasdiqlab,
    // yangi doimiy token bilan javob qaytarganda - shu tokenni saqlab
    // qolamiz, shunda keyingi safar QR/tasdiqlashsiz avtomatik ulanadi.
    LaunchedEffect(Unit) {
        PresenterSession.client.events.collect { envelope ->
            val key = pendingKey
            if (envelope.type == "pairResult" && envelope.success == true && envelope.token != null && key != null) {
                store.saveToken(key, envelope.token)
            }
        }
    }

    val qrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val ip = result.data?.getStringExtra(QrScanActivity.EXTRA_IP)
            val port = result.data?.getIntExtra(QrScanActivity.EXTRA_PORT, -1)
            val token = result.data?.getStringExtra(QrScanActivity.EXTRA_TOKEN)
            if (!ip.isNullOrEmpty() && port != null && port > 0 && !token.isNullOrEmpty()) {
                val key = "$ip:$port"
                store.saveToken(key, token)
                pendingKey = key
                PresenterSession.connect(PairingPayload(ip, port, token))
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            qrLauncher.launch(Intent(context, QrScanActivity::class.java))
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)) {
        Text("Prezenter", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    qrLauncher.launch(Intent(context, QrScanActivity::class.java))
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        ) {
            Text("QR kodni skanerlash")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Tarmoqdagi kompyuterlar", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(computers) { computer ->
                val key = "${computer.host}:${computer.port}"
                val savedToken = store.getToken(key)
                val isPendingThis = pendingKey == key &&
                    (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.PAIRING)
                ListItem(
                    headlineContent = { Text(computer.name) },
                    supportingContent = {
                        val text = when {
                            isPendingThis -> "Kompyuterda tasdiqlashni kuting..."
                            savedToken != null -> "Ulanish uchun bosing"
                            else -> "Ulanish uchun bosing (kompyuterda tasdiqlash so'raladi)"
                        }
                        Text(text)
                    },
                    modifier = Modifier.clickable(enabled = !isPendingThis) {
                        pendingKey = key
                        PresenterSession.connect(PairingPayload(computer.host, computer.port, savedToken))
                    }
                )
            }
        }

        if (connectionState == ConnectionState.ERROR) {
            Spacer(modifier = Modifier.height(16.dp))
            val errorText = lastError ?: "noma'lum sabab"
            Text(
                "Ulanishda xatolik: $errorText\n\n" +
                    "Kompyuterda Windows Firewall shu dasturga ruxsat berganiga ishonch hosil qiling.",
                color = Color.Red
            )
        }
    }
}

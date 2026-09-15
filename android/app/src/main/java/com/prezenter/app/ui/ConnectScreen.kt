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
import androidx.compose.runtime.remember
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

    DisposableEffect(Unit) {
        discovery.startDiscovery()
        onDispose { discovery.stopDiscovery() }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            onConnected()
        }
    }

    val qrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val ip = result.data?.getStringExtra(QrScanActivity.EXTRA_IP)
            val port = result.data?.getIntExtra(QrScanActivity.EXTRA_PORT, -1)
            val token = result.data?.getStringExtra(QrScanActivity.EXTRA_TOKEN)
            if (!ip.isNullOrEmpty() && port != null && port > 0 && !token.isNullOrEmpty()) {
                store.saveToken("$ip:$port", token)
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
                ListItem(
                    headlineContent = { Text(computer.name) },
                    supportingContent = {
                        Text(if (savedToken != null) "Ulanish uchun bosing" else "Avval QR orqali qo'shing")
                    },
                    modifier = Modifier.clickable(enabled = savedToken != null) {
                        if (savedToken != null) {
                            PresenterSession.connect(PairingPayload(computer.host, computer.port, savedToken))
                        }
                    }
                )
            }
        }

        if (connectionState == ConnectionState.ERROR) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ulanishda xatolik yuz berdi, qayta urinib ko'ring", color = Color.Red)
        }
    }
}

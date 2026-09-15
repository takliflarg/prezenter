package com.prezenter.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.prezenter.app.network.ConnectionState
import com.prezenter.app.network.PresenterSession
import com.prezenter.app.ui.ConnectScreen
import com.prezenter.app.ui.ControlScreen
import com.prezenter.app.ui.theme.PrezenterTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* natija muhim emas */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        PresenterSession.reconnectIfPossible()

        setContent {
            PrezenterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrezenterApp()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun PrezenterApp() {
    val connectionState by PresenterSession.client.state.collectAsState()

    if (connectionState == ConnectionState.CONNECTED) {
        ControlScreen()
    } else {
        ConnectScreen(onConnected = { /* state o'zgarishi bilan avtomatik ControlScreen ko'rsatiladi */ })
    }
}

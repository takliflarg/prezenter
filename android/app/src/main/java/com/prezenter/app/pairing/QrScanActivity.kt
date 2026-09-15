package com.prezenter.app.pairing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.prezenter.app.network.PairingPayload
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Kompyuter ekranidagi QR kodni skanerlaydi va undagi IP/port/token
 * ma'lumotini chaqiruvchi ekranga (ConnectScreen) qaytaradi.
 */
class QrScanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QrScannerScreen(onResult = ::finishWithResult)
        }
    }

    private fun finishWithResult(rawValue: String) {
        val payload = runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<PairingPayload>(rawValue) }
            .getOrNull()

        if (payload != null) {
            val result = Intent()
                .putExtra(EXTRA_IP, payload.ip)
                .putExtra(EXTRA_PORT, payload.port)
                .putExtra(EXTRA_TOKEN, payload.token)
            setResult(RESULT_OK, result)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    companion object {
        const val EXTRA_IP = "ip"
        const val EXTRA_PORT = "port"
        const val EXTRA_TOKEN = "token"
    }
}

@Composable
private fun QrScannerScreen(onResult: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { context ->
            val previewView = PreviewView(context)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(context), QrCodeAnalyzer(onResult))
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(context))

            previewView
        }, modifier = Modifier.fillMaxSize())

        Text(text = "Kompyuter ekranidagi QR kodni kameraga tuting")
    }
}

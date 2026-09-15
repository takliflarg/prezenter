package com.prezenter.app.pairing

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Har bir kamera freymida QR kod izlaydi. Topilgan birinchi to'g'ri
 * qiymat uchun onQrCodeFound chaqiriladi, keyin analyzer o'zini
 * to'xtatadi (qayta-qayta chaqirilmasligi uchun).
 */
class QrCodeAnalyzer(private val onQrCodeFound: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var found = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (found || mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull { it.valueType == Barcode.TYPE_TEXT || it.rawValue != null }
                    ?.rawValue
                if (value != null && !found) {
                    found = true
                    onQrCodeFound(value)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

package com.traduya.feature.internetcall.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Código QR del enlace: la otra persona lo escanea con la cámara y llama. */
@Composable
internal fun QrCode(content: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1))
        Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    setPixel(x, y, if (matrix[x, y]) 0xFF0B3954.toInt() else 0xFFFFFFFF.toInt())
                }
            }
        }.asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = "Código QR del enlace de llamada",
        filterQuality = FilterQuality.None,
        modifier = modifier,
    )
}

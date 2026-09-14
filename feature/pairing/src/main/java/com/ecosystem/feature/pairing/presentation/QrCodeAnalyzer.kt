package com.ecosystem.feature.pairing.presentation

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Decodes QR codes from camera frames and reports at most one result per instance. After
 * the first result, or after [disable], every frame is closed immediately, so a scanner
 * session can never start two pairing attempts.
 */
class QrCodeAnalyzer(private val onDecoded: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }
    private val delivered = AtomicBoolean(false)

    @Volatile
    private var enabled = true

    fun disable() {
        enabled = false
    }

    override fun analyze(image: ImageProxy) {
        image.use { frame ->
            if (!enabled || delivered.get() || frame.format != ImageFormat.YUV_420_888) return
            val plane = frame.planes[0]
            val luminance = LuminancePlane.extract(plane.buffer, plane.rowStride, plane.pixelStride, frame.width, frame.height)
            val source = PlanarYUVLuminanceSource(luminance, frame.width, frame.height, 0, 0, frame.width, frame.height, false)
            val text = try {
                reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
            } catch (e: ReaderException) {
                null
            } finally {
                reader.reset()
            }
            if (text != null && enabled && delivered.compareAndSet(false, true)) onDecoded(text)
        }
    }
}

/** Copies the Y plane into a tightly packed `width * height` array, honouring row and pixel stride. */
object LuminancePlane {
    fun extract(buffer: ByteBuffer, rowStride: Int, pixelStride: Int, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height)
        val source = buffer.duplicate().apply { rewind() }
        if (pixelStride == 1 && rowStride == width) {
            source.get(out, 0, minOf(out.size, source.remaining()))
            return out
        }
        for (row in 0 until height) {
            val rowStart = row * rowStride
            for (column in 0 until width) {
                val index = rowStart + column * pixelStride
                if (index < source.limit()) out[row * width + column] = source.get(index)
            }
        }
        return out
    }
}

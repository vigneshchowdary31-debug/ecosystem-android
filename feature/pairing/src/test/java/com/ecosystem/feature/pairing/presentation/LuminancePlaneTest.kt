package com.ecosystem.feature.pairing.presentation

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class LuminancePlaneTest {

    @Test
    fun `tightly packed plane is copied as is`() {
        val data = ByteArray(6) { it.toByte() }
        assertArrayEquals(data, LuminancePlane.extract(ByteBuffer.wrap(data), rowStride = 3, pixelStride = 1, width = 3, height = 2))
    }

    @Test
    fun `row padding is skipped`() {
        // 3x2 image stored with a row stride of 5 (2 padding bytes per row).
        val padded = byteArrayOf(1, 2, 3, 99, 99, 4, 5, 6, 99, 99)
        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4, 5, 6),
            LuminancePlane.extract(ByteBuffer.wrap(padded), rowStride = 5, pixelStride = 1, width = 3, height = 2),
        )
    }

    @Test
    fun `pixel stride is honoured`() {
        val interleaved = byteArrayOf(1, 0, 2, 0, 3, 0, 4, 0)
        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4),
            LuminancePlane.extract(ByteBuffer.wrap(interleaved), rowStride = 4, pixelStride = 2, width = 2, height = 2),
        )
    }

    @Test
    fun `last row without trailing padding is handled`() {
        // Camera buffers often omit the padding after the final row.
        val data = byteArrayOf(1, 2, 3, 99, 4, 5, 6)
        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4, 5, 6),
            LuminancePlane.extract(ByteBuffer.wrap(data), rowStride = 4, pixelStride = 1, width = 3, height = 2),
        )
    }
}

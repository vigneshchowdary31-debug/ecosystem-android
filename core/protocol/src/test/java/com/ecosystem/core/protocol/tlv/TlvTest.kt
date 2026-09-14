package com.ecosystem.core.protocol.tlv

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TlvTest {

    @Test
    fun `encode uses tag then big-endian length then value`() {
        assertArrayEquals(byteArrayOf(0x12, 0x00, 0x02, 0x0A, 0x0B), Tlv.encode(0x12, byteArrayOf(0x0A, 0x0B)))
        assertArrayEquals(byteArrayOf(0x05, 0x00, 0x00), Tlv.encode(0x05, ByteArray(0)))
    }

    @Test
    fun `long values use both length bytes`() {
        val encoded = Tlv.encode(0x01, ByteArray(300))
        assertEquals(0x01, encoded[1].toInt())
        assertEquals(0x2C, encoded[2].toInt())
    }

    @Test
    fun `strict reader accepts the exact schema`() {
        val bytes = TlvBuilder().add(1, ByteArray(2)).add(2, ByteArray(3)).build()
        val reader = StrictTlvReader(bytes)
        assertEquals(2, reader.required(1, 2).size)
        assertNull(reader.optional(9, setOf(1)))
        assertEquals(3, reader.required(2, 1..4).size)
        reader.finish()
    }

    @Test(expected = TlvFormatException::class)
    fun `strict reader rejects reordered fields`() {
        val bytes = TlvBuilder().add(2, ByteArray(1)).add(1, ByteArray(1)).build()
        StrictTlvReader(bytes).required(1, 1)
    }

    @Test(expected = TlvFormatException::class)
    fun `strict reader rejects wrong lengths`() {
        StrictTlvReader(Tlv.encode(1, ByteArray(31))).required(1, 32)
    }

    @Test(expected = TlvFormatException::class)
    fun `strict reader rejects trailing fields`() {
        val reader = StrictTlvReader(TlvBuilder().add(1, ByteArray(1)).add(7, ByteArray(1)).build())
        reader.required(1, 1)
        reader.finish()
    }

    @Test(expected = TlvFormatException::class)
    fun `truncated value is rejected`() {
        Tlv.decode(byteArrayOf(0x01, 0x00, 0x05, 0x00))
    }

    @Test(expected = TlvFormatException::class)
    fun `truncated header is rejected`() {
        Tlv.decode(byteArrayOf(0x01, 0x00))
    }
}

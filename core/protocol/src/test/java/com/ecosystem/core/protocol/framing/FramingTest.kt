package com.ecosystem.core.protocol.framing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FramingTest {

    private fun frame(type: Int, size: Int, seed: Int = 1) = Frame(type, ByteArray(size) { (it * seed + type).toByte() })

    private fun expectFailure(reason: FramingException.Reason, block: () -> Unit) {
        try {
            block()
            fail("Expected FramingException($reason)")
        } catch (e: FramingException) {
            assertEquals(reason, e.reason)
        }
    }

    @Test
    fun `crc32 matches the IEEE check value`() {
        val data = "123456789".toByteArray(Charsets.US_ASCII)
        assertEquals(0xCBF43926L, FrameCodec.crc32(data, 0, data.size))
    }

    @Test
    fun `encoded frame has the documented layout`() {
        val bytes = FrameCodec.encode(Frame(0x01, byteArrayOf(0x0A, 0x0B)))
        assertEquals(6 + 2 + 4, bytes.size)
        assertEquals(0x43, bytes[0].toInt())
        assertEquals(0x46, bytes[1].toInt())
        assertEquals(0x02, bytes[2].toInt())
        assertEquals(0x01, bytes[3].toInt())
        assertEquals(0x00, bytes[4].toInt())
        assertEquals(0x02, bytes[5].toInt())
    }

    @Test
    fun `single frame round-trips`() {
        val original = frame(0x03, 300)
        val frames = FrameReassembler().append(FrameCodec.encode(original))
        assertEquals(listOf(original), frames)
    }

    @Test
    fun `empty payload round-trips`() {
        val original = Frame(0x7F, ByteArray(0))
        assertEquals(listOf(original), FrameReassembler().append(FrameCodec.encode(original)))
    }

    @Test
    fun `frame fragmented into one-byte packets completes only on the last byte`() {
        val original = frame(0x04, 200)
        val bytes = FrameCodec.encode(original)
        val reassembler = FrameReassembler()
        bytes.dropLast(1).forEach { assertTrue(reassembler.append(byteArrayOf(it)).isEmpty()) }
        assertEquals(listOf(original), reassembler.append(byteArrayOf(bytes.last())))
        assertEquals(0, reassembler.bufferedByteCount)
    }

    @Test
    fun `multiple frames in one packet are all returned in order`() {
        val frames = listOf(frame(0x01, 64), frame(0x02, 0), frame(0x03, 250))
        val stream = frames.map(FrameCodec::encode).reduce { a, b -> a + b }
        assertEquals(frames, FrameReassembler().append(stream))
    }

    @Test
    fun `packet boundaries that straddle frames are handled`() {
        val frames = listOf(frame(0x01, 70), frame(0x02, 33), frame(0x05, 32))
        val stream = frames.map(FrameCodec::encode).reduce { a, b -> a + b }
        for (chunk in listOf(1, 5, 20, 37, 182, 512)) {
            val reassembler = FrameReassembler()
            val received = Fragmenter.split(stream, chunk).flatMap { reassembler.append(it) }
            assertEquals("chunk=$chunk", frames, received)
        }
    }

    @Test
    fun `partial frame is buffered until complete`() {
        val bytes = FrameCodec.encode(frame(0x02, 64))
        val reassembler = FrameReassembler()
        assertTrue(reassembler.append(bytes.copyOfRange(0, 10)).isEmpty())
        assertEquals(10, reassembler.bufferedByteCount)
        assertEquals(1, reassembler.append(bytes.copyOfRange(10, bytes.size)).size)
    }

    @Test
    fun `bad magic fails immediately and poisons the reassembler`() {
        val reassembler = FrameReassembler()
        expectFailure(FramingException.Reason.BAD_MAGIC) { reassembler.append("auth:abc".toByteArray()) }
        expectFailure(FramingException.Reason.FAILED_EARLIER) { reassembler.append(FrameCodec.encode(frame(1, 1))) }
    }

    @Test
    fun `legacy base64 key write is rejected as bad magic`() {
        // A v1 peer sends a bare base64 string; v2 must not misinterpret it.
        expectFailure(FramingException.Reason.BAD_MAGIC) {
            FrameReassembler().append("u514c3+fG56MzdV8q2+wKWhS5g7Y2Lq6m1d3P5R7Z8Q=".toByteArray())
        }
    }

    @Test
    fun `unsupported version is rejected`() {
        val bytes = FrameCodec.encode(frame(0x01, 4))
        bytes[2] = 0x03
        expectFailure(FramingException.Reason.UNSUPPORTED_VERSION) { FrameReassembler().append(bytes) }
    }

    @Test
    fun `malformed length beyond the maximum is rejected before the payload arrives`() {
        val header = byteArrayOf(0x43, 0x46, 0x02, 0x01, 0x04, 0x01) // length 1025
        expectFailure(FramingException.Reason.PAYLOAD_TOO_LARGE) { FrameReassembler().append(header) }
    }

    @Test
    fun `corrupted payload fails the CRC`() {
        val bytes = FrameCodec.encode(frame(0x03, 40))
        bytes[10] = (bytes[10].toInt() xor 0x01).toByte()
        expectFailure(FramingException.Reason.CRC_MISMATCH) { FrameReassembler().append(bytes) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `oversized payload cannot be encoded`() {
        Frame(0x01, ByteArray(FrameCodec.MAX_PAYLOAD_SIZE + 1))
    }

    @Test
    fun `largest payload round-trips at every packet size`() {
        val original = frame(0x03, FrameCodec.MAX_PAYLOAD_SIZE)
        for (mtu in listOf(23, 185, 247, 517)) {
            val packetSize = AttMtu.maxPayloadPerPacket(mtu)
            val packets = Fragmenter.split(FrameCodec.encode(original), packetSize)
            assertTrue(packets.all { it.size <= packetSize })
            val reassembler = FrameReassembler()
            assertEquals(listOf(original), packets.flatMap { reassembler.append(it) })
        }
    }

    @Test
    fun `att payload size follows the negotiated mtu`() {
        assertEquals(20, AttMtu.maxPayloadPerPacket(23))
        assertEquals(20, AttMtu.maxPayloadPerPacket(10))
        assertEquals(182, AttMtu.maxPayloadPerPacket(185))
        assertEquals(244, AttMtu.maxPayloadPerPacket(247))
        assertEquals(512, AttMtu.maxPayloadPerPacket(517))
        assertEquals(512, AttMtu.maxPayloadPerPacket(1000))
    }

    @Test
    fun `fragmenter covers every byte exactly once`() {
        val bytes = ByteArray(101) { it.toByte() }
        val chunks = Fragmenter.split(bytes, 20)
        assertEquals(6, chunks.size)
        assertEquals(1, chunks.last().size)
        assertTrue(chunks.reduce { a, b -> a + b }.contentEquals(bytes))
    }
}

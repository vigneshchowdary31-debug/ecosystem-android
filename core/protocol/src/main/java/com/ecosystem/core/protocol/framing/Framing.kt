package com.ecosystem.core.protocol.framing

import com.ecosystem.core.protocol.ProtocolConstants
import java.util.zip.CRC32

/** One application message. [type] is a message-type byte; [payload] is at most [FrameCodec.MAX_PAYLOAD_SIZE] bytes. */
class Frame(val type: Int, val payload: ByteArray) {
    init {
        require(type in 0..0xFF) { "Frame type out of range: $type" }
        require(payload.size <= FrameCodec.MAX_PAYLOAD_SIZE) { "Frame payload too large: ${payload.size}" }
    }

    override fun equals(other: Any?): Boolean =
        other is Frame && other.type == type && other.payload.contentEquals(payload)

    override fun hashCode(): Int = 31 * type + payload.contentHashCode()

    override fun toString(): String = "Frame(type=0x%02x, payload=%d bytes)".format(type, payload.size)
}

/**
 * Frame layout (all integers big-endian):
 *
 * ```
 * offset  size  field
 * 0       2     magic 0x43 0x46 ("CF")
 * 2       1     protocol version (0x02)
 * 3       1     message type
 * 4       2     payload length N (0..1024)
 * 6       N     payload
 * 6+N     4     CRC-32 (IEEE 802.3, as java.util.zip.CRC32 / zlib) of bytes [0, 6+N)
 * ```
 *
 * Frames form a byte stream per direction. The stream may be cut into BLE packets at any
 * byte boundary and several frames may share one packet.
 */
object FrameCodec {
    const val MAGIC_0: Byte = 0x43
    const val MAGIC_1: Byte = 0x46
    const val HEADER_SIZE = 6
    const val CRC_SIZE = 4
    const val MAX_PAYLOAD_SIZE = 1024
    const val MAX_FRAME_SIZE = HEADER_SIZE + MAX_PAYLOAD_SIZE + CRC_SIZE

    fun encode(frame: Frame): ByteArray {
        val length = frame.payload.size
        val out = ByteArray(HEADER_SIZE + length + CRC_SIZE)
        out[0] = MAGIC_0
        out[1] = MAGIC_1
        out[2] = ProtocolConstants.PROTOCOL_VERSION.toByte()
        out[3] = frame.type.toByte()
        out[4] = (length ushr 8).toByte()
        out[5] = length.toByte()
        frame.payload.copyInto(out, HEADER_SIZE)
        val crc = crc32(out, 0, HEADER_SIZE + length)
        val crcOffset = HEADER_SIZE + length
        out[crcOffset] = (crc ushr 24).toByte()
        out[crcOffset + 1] = (crc ushr 16).toByte()
        out[crcOffset + 2] = (crc ushr 8).toByte()
        out[crcOffset + 3] = crc.toByte()
        return out
    }

    internal fun crc32(bytes: ByteArray, offset: Int, length: Int): Long {
        val crc = CRC32()
        crc.update(bytes, offset, length)
        return crc.value
    }
}

class FramingException(val reason: Reason, message: String) : Exception(message) {
    enum class Reason { BAD_MAGIC, UNSUPPORTED_VERSION, PAYLOAD_TOO_LARGE, CRC_MISMATCH, FAILED_EARLIER }
}

/**
 * Rebuilds frames from any chunking of the byte stream. A frame is complete only when the
 * number of bytes announced by its length field (plus the CRC) has arrived; delimiters and
 * callback boundaries are never used.
 *
 * Every error is fatal: after one failure the reassembler rejects all further input and the
 * session must be aborted, because a byte stream cannot be resynchronised safely.
 *
 * Not thread-safe; callers serialise access.
 */
class FrameReassembler(private val maxPayloadSize: Int = FrameCodec.MAX_PAYLOAD_SIZE) {
    private var buffer = ByteArray(0)
    private var failure: FramingException? = null

    val bufferedByteCount: Int get() = buffer.size

    /** Adds one received packet and returns every frame it completes, in order. */
    fun append(chunk: ByteArray): List<Frame> {
        failure?.let {
            throw FramingException(FramingException.Reason.FAILED_EARLIER, "Reassembler already failed: ${it.message}")
        }
        return try {
            drain(chunk)
        } catch (e: FramingException) {
            failure = e
            buffer = ByteArray(0)
            throw e
        }
    }

    private fun drain(chunk: ByteArray): List<Frame> {
        buffer += chunk
        val frames = ArrayList<Frame>()
        while (true) {
            // Validate the header byte by byte so a foreign stream fails on its first bytes.
            if (buffer.isNotEmpty() && buffer[0] != FrameCodec.MAGIC_0) badMagic()
            if (buffer.size >= 2 && buffer[1] != FrameCodec.MAGIC_1) badMagic()
            if (buffer.size >= 3) {
                val version = buffer[2].toInt() and 0xFF
                if (version != ProtocolConstants.PROTOCOL_VERSION) {
                    throw FramingException(
                        FramingException.Reason.UNSUPPORTED_VERSION,
                        "Unsupported frame version $version"
                    )
                }
            }
            if (buffer.size < FrameCodec.HEADER_SIZE) break

            val length = ((buffer[4].toInt() and 0xFF) shl 8) or (buffer[5].toInt() and 0xFF)
            if (length > maxPayloadSize) {
                throw FramingException(FramingException.Reason.PAYLOAD_TOO_LARGE, "Frame payload of $length bytes exceeds $maxPayloadSize")
            }
            val total = FrameCodec.HEADER_SIZE + length + FrameCodec.CRC_SIZE
            if (buffer.size < total) break

            val crcOffset = FrameCodec.HEADER_SIZE + length
            val expected = FrameCodec.crc32(buffer, 0, crcOffset)
            val actual = ((buffer[crcOffset].toLong() and 0xFF) shl 24) or
                ((buffer[crcOffset + 1].toLong() and 0xFF) shl 16) or
                ((buffer[crcOffset + 2].toLong() and 0xFF) shl 8) or
                (buffer[crcOffset + 3].toLong() and 0xFF)
            if (expected != actual) {
                throw FramingException(FramingException.Reason.CRC_MISMATCH, "Frame CRC mismatch")
            }
            frames += Frame(buffer[3].toInt() and 0xFF, buffer.copyOfRange(FrameCodec.HEADER_SIZE, crcOffset))
            buffer = buffer.copyOfRange(total, buffer.size)
        }
        return frames
    }

    private fun badMagic(): Nothing =
        throw FramingException(FramingException.Reason.BAD_MAGIC, "Stream does not start with a ConnectFlow frame")
}

/** Splits an encoded frame stream into packets no larger than the negotiated ATT payload. */
object Fragmenter {
    fun split(bytes: ByteArray, maxChunkSize: Int): List<ByteArray> {
        require(maxChunkSize > 0) { "Chunk size must be positive" }
        if (bytes.isEmpty()) return emptyList()
        return (bytes.indices step maxChunkSize).map { start ->
            bytes.copyOfRange(start, minOf(start + maxChunkSize, bytes.size))
        }
    }
}

/**
 * ATT sizing. A Write Request and a Handle Value Notification each carry at most
 * `ATT_MTU - 3` bytes of attribute value, and an attribute value is at most 512 bytes.
 * With the default ATT_MTU of 23 that is 20 bytes per packet.
 */
object AttMtu {
    const val DEFAULT_ATT_MTU = 23
    const val REQUESTED_ATT_MTU = 517
    const val ATT_HEADER_SIZE = 3
    const val MAX_ATTRIBUTE_VALUE_SIZE = 512

    fun maxPayloadPerPacket(negotiatedMtu: Int): Int =
        (maxOf(negotiatedMtu, DEFAULT_ATT_MTU) - ATT_HEADER_SIZE).coerceAtMost(MAX_ATTRIBUTE_VALUE_SIZE)
}

package com.ecosystem.core.protocol.tlv

import java.io.ByteArrayOutputStream

/**
 * Length-prefixed field encoding used by handshake messages and by the transcript:
 * `tag (1 byte) || length (2 bytes, unsigned big-endian) || value`.
 *
 * Because every field carries its own length there are no delimiters to escape and no
 * ambiguity between adjacent fields.
 */
object Tlv {
    const val HEADER_SIZE = 3
    const val MAX_VALUE_LENGTH = 0xFFFF

    fun encode(tag: Int, value: ByteArray): ByteArray {
        require(tag in 0..0xFF) { "TLV tag out of range: $tag" }
        require(value.size <= MAX_VALUE_LENGTH) { "TLV value too long: ${value.size}" }
        val out = ByteArray(HEADER_SIZE + value.size)
        out[0] = tag.toByte()
        out[1] = (value.size ushr 8).toByte()
        out[2] = value.size.toByte()
        value.copyInto(out, HEADER_SIZE)
        return out
    }

    fun decode(bytes: ByteArray): List<TlvField> {
        val fields = ArrayList<TlvField>()
        var i = 0
        while (i < bytes.size) {
            if (bytes.size - i < HEADER_SIZE) throw TlvFormatException("Truncated TLV header at offset $i")
            val tag = bytes[i].toInt() and 0xFF
            val length = ((bytes[i + 1].toInt() and 0xFF) shl 8) or (bytes[i + 2].toInt() and 0xFF)
            i += HEADER_SIZE
            if (bytes.size - i < length) throw TlvFormatException("Truncated TLV value for tag 0x%02x".format(tag))
            fields += TlvField(tag, bytes.copyOfRange(i, i + length))
            i += length
        }
        return fields
    }
}

class TlvField(val tag: Int, val value: ByteArray)

class TlvFormatException(message: String) : Exception(message)

class TlvBuilder {
    private val out = ByteArrayOutputStream()

    fun add(tag: Int, value: ByteArray): TlvBuilder {
        out.write(Tlv.encode(tag, value))
        return this
    }

    fun addIfPresent(tag: Int, value: ByteArray?): TlvBuilder {
        if (value != null) add(tag, value)
        return this
    }

    fun build(): ByteArray = out.toByteArray()
}

/**
 * Reads fields in the exact order a message schema declares and rejects missing fields,
 * wrong lengths, reordering, duplicates and trailing unknown fields.
 */
class StrictTlvReader(bytes: ByteArray) {
    private val fields = Tlv.decode(bytes)
    private var index = 0

    fun required(tag: Int, length: Int): ByteArray = required(tag, length..length)

    fun required(tag: Int, lengths: IntRange): ByteArray {
        val field = fields.getOrNull(index)
            ?: throw TlvFormatException("Missing field 0x%02x".format(tag))
        if (field.tag != tag) {
            throw TlvFormatException("Expected field 0x%02x but found 0x%02x".format(tag, field.tag))
        }
        if (field.value.size !in lengths) {
            throw TlvFormatException("Field 0x%02x has invalid length ${field.value.size}".format(tag))
        }
        index++
        return field.value
    }

    fun optional(tag: Int, allowedLengths: Set<Int>): ByteArray? {
        val field = fields.getOrNull(index)
        if (field == null || field.tag != tag) return null
        if (field.value.size !in allowedLengths) {
            throw TlvFormatException("Field 0x%02x has invalid length ${field.value.size}".format(tag))
        }
        index++
        return field.value
    }

    fun finish() {
        if (index != fields.size) {
            throw TlvFormatException("Unexpected field 0x%02x".format(fields[index].tag))
        }
    }
}

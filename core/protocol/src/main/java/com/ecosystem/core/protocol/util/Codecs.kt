package com.ecosystem.core.protocol.util

import com.ecosystem.core.protocol.ProtocolConstants
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Base64

/** Strict RFC 4648 Base64. Decoders return null for anything that is not the canonical encoding. */
object Base64Codec {
    private val URL_ALPHABET = Regex("^[A-Za-z0-9_-]*$")
    private val STANDARD_ALPHABET = Regex("^[A-Za-z0-9+/]*={0,2}$")

    fun encodeUrlNoPadding(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun decodeUrlNoPadding(text: String): ByteArray? {
        if (!URL_ALPHABET.matches(text) || text.length % 4 == 1) return null
        val decoded = try {
            Base64.getUrlDecoder().decode(text)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return if (encodeUrlNoPadding(decoded) == text) decoded else null
    }

    fun encodeStandard(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun decodeStandard(text: String): ByteArray? {
        if (text.length % 4 != 0 || !STANDARD_ALPHABET.matches(text)) return null
        val decoded = try {
            Base64.getDecoder().decode(text)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return if (encodeStandard(decoded) == text) decoded else null
    }
}

/** RFC 3986 percent-encoding restricted to what the pairing QR needs. */
object PercentCodec {
    private const val UNRESERVED =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    /** Encodes every byte outside the RFC 3986 unreserved set as `%XX` (upper-case hex). */
    fun encode(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            val c = (b.toInt() and 0xFF).toChar()
            if (c in UNRESERVED) {
                out.append(c)
            } else {
                out.append('%').append("%02X".format(b.toInt() and 0xFF))
            }
        }
        return out.toString()
    }

    /** Decodes `%XX` escapes; every other character stands for its own ASCII byte. Null on a bad escape. */
    fun decode(text: String): ByteArray? {
        val out = java.io.ByteArrayOutputStream(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '%') {
                if (i + 2 >= text.length) return null
                val hi = Character.digit(text[i + 1], 16)
                val lo = Character.digit(text[i + 2], 16)
                if (hi < 0 || lo < 0) return null
                out.write((hi shl 4) or lo)
                i += 3
            } else {
                if (c.code > 0x7E) return null
                out.write(c.code)
                i += 1
            }
        }
        return out.toByteArray()
    }
}

/** Device-name rules shared by the QR code and the ANDROID_AUTH message. */
object DeviceNames {
    const val FALLBACK_NAME = "Android device"

    /** Validates wire bytes: strict UTF-8, 1..128 bytes, not blank, no control characters. */
    fun decodeWire(bytes: ByteArray): String? {
        if (bytes.isEmpty() || bytes.size > ProtocolConstants.MAX_DEVICE_NAME_BYTES) return null
        val text = strictUtf8(bytes) ?: return null
        if (text.isBlank() || text.any { Character.isISOControl(it) }) return null
        return text
    }

    /** Produces valid wire bytes for a local name: strips control characters and truncates on a code-point boundary. */
    fun toWireBytes(name: String): ByteArray {
        val cleaned = name.filterNot { Character.isISOControl(it) }.trim().ifEmpty { FALLBACK_NAME }
        val out = StringBuilder()
        var byteCount = 0
        var index = 0
        while (index < cleaned.length) {
            val codePoint = cleaned.codePointAt(index)
            val chunk = String(Character.toChars(codePoint))
            val size = chunk.toByteArray(Charsets.UTF_8).size
            if (byteCount + size > ProtocolConstants.MAX_DEVICE_NAME_BYTES) break
            out.append(chunk)
            byteCount += size
            index += Character.charCount(codePoint)
        }
        return out.toString().trim().ifEmpty { FALLBACK_NAME }.toByteArray(Charsets.UTF_8)
    }

    private fun strictUtf8(bytes: ByteArray): String? = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: CharacterCodingException) {
        null
    }
}

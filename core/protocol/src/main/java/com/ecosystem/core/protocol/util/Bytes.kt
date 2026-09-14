package com.ecosystem.core.protocol.util

import java.nio.ByteBuffer
import java.util.UUID

/** Overwrites the array with zeros to shorten the lifetime of key material in memory. */
fun ByteArray.wipe() = fill(0)

fun ByteArray.isAllZero(): Boolean {
    var acc = 0
    for (b in this) acc = acc or b.toInt()
    return acc == 0
}

/** Compares two arrays in time that depends only on their length, never on their contents. */
fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) return false
    var diff = 0
    for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
    return diff == 0
}

private val HEX_DIGITS = "0123456789abcdef".toCharArray()

fun ByteArray.toHex(): String {
    val out = CharArray(size * 2)
    for (i in indices) {
        val v = this[i].toInt() and 0xFF
        out[i * 2] = HEX_DIGITS[v ushr 4]
        out[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
    }
    return String(out)
}

fun hexToBytes(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "Odd-length hex string" }
    return ByteArray(hex.length / 2) { i ->
        val hi = Character.digit(hex[i * 2], 16)
        val lo = Character.digit(hex[i * 2 + 1], 16)
        require(hi >= 0 && lo >= 0) { "Invalid hex digit" }
        ((hi shl 4) or lo).toByte()
    }
}

/** UUID helpers. On the wire a device ID is its 16 RFC 4122 bytes, so text case never matters. */
object Uuids {
    private val CANONICAL =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /** Parses only the 36-character 8-4-4-4-12 form; [UUID.fromString] alone accepts sloppier input. */
    fun parseCanonical(text: String): UUID? =
        if (CANONICAL.matches(text)) UUID.fromString(text) else null

    fun toBytes(uuid: UUID): ByteArray =
        ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()

    fun fromBytes(bytes: ByteArray): UUID {
        require(bytes.size == 16) { "A UUID is 16 bytes" }
        val buffer = ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long)
    }

    /** Lower-case text form used for storage and comparisons on Android. */
    fun canonical(uuid: UUID): String = uuid.toString()
}

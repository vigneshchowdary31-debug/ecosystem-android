package com.ecosystem.core.protocol.qr

import com.ecosystem.core.protocol.ProtocolConstants
import com.ecosystem.core.protocol.util.Base64Codec
import com.ecosystem.core.protocol.util.DeviceNames
import com.ecosystem.core.protocol.util.PercentCodec
import com.ecosystem.core.protocol.util.Uuids
import java.util.UUID

/** The Mac identity shown in its pairing QR code. Contains no MAC address and no session secret. */
class QrPairingPayload(
    val version: Int,
    val deviceId: UUID,
    val deviceName: String,
    val advertisingIdentifier: UUID,
    val identityPublicKey: ByteArray,
) {
    init {
        require(identityPublicKey.size == ProtocolConstants.ED25519_PUBLIC_KEY_SIZE)
    }
}

enum class QrError {
    NOT_A_PAIRING_CODE,
    LEGACY_FORMAT,
    UNSUPPORTED_VERSION,
    TOO_LONG,
    MALFORMED,
    MISSING_FIELD,
    DUPLICATE_FIELD,
    UNKNOWN_FIELD,
    INVALID_DEVICE_ID,
    INVALID_DEVICE_NAME,
    INVALID_ADVERTISING_ID,
    INVALID_PUBLIC_KEY,
}

sealed interface QrParseResult {
    class Valid(val payload: QrPairingPayload) : QrParseResult
    class Invalid(val error: QrError, val detail: String) : QrParseResult
}

/**
 * Versioned pairing QR format:
 *
 * ```
 * connectflow://pair?v=2&id=<uuid>&name=<percent-encoded UTF-8>&adv=<uuid>&pk=<base64url, no padding>
 * ```
 *
 * Parsing is strict: ASCII only, at most [MAX_LENGTH] characters, every field exactly once,
 * no unknown fields for v2, canonical UUIDs, a device name of 1..128 UTF-8 bytes and a public
 * key that decodes to exactly 32 bytes.
 */
object QrPayloadCodec {
    const val PREFIX = "connectflow://pair?"
    const val LEGACY_PREFIX = "identity_continuity:"
    const val MAX_LENGTH = 512

    private val FIELDS = listOf("v", "id", "name", "adv", "pk")
    private val KEY_PATTERN = Regex("^[a-z]{1,8}$")
    private val VERSION_PATTERN = Regex("^[1-9][0-9]{0,2}$")
    private const val ALLOWED_VALUE_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~!$'()*+,;:@/?%"
    private const val BASE64URL_KEY_LENGTH = 43

    fun encode(payload: QrPairingPayload): String {
        val nameBytes = DeviceNames.toWireBytes(payload.deviceName)
        return PREFIX +
            "v=${payload.version}" +
            "&id=${Uuids.canonical(payload.deviceId)}" +
            "&name=${PercentCodec.encode(nameBytes)}" +
            "&adv=${Uuids.canonical(payload.advertisingIdentifier)}" +
            "&pk=${Base64Codec.encodeUrlNoPadding(payload.identityPublicKey)}"
    }

    fun parse(text: String): QrParseResult {
        if (text.startsWith(LEGACY_PREFIX)) {
            return invalid(QrError.LEGACY_FORMAT, "Legacy v1 pairing code; the Mac app must be updated")
        }
        if (!text.startsWith(PREFIX)) return invalid(QrError.NOT_A_PAIRING_CODE, "Not a ConnectFlow pairing code")
        if (text.length > MAX_LENGTH) return invalid(QrError.TOO_LONG, "Pairing code longer than $MAX_LENGTH characters")
        if (text.any { it.code !in 0x21..0x7E }) return invalid(QrError.MALFORMED, "Pairing code contains non-printable characters")

        val query = text.substring(PREFIX.length)
        if (query.isEmpty()) return invalid(QrError.MALFORMED, "Pairing code has no fields")

        val fields = LinkedHashMap<String, String>()
        for (pair in query.split('&')) {
            val separator = pair.indexOf('=')
            if (separator <= 0 || pair.indexOf('=', separator + 1) != -1) {
                return invalid(QrError.MALFORMED, "Malformed field")
            }
            val key = pair.substring(0, separator)
            val value = pair.substring(separator + 1)
            if (!KEY_PATTERN.matches(key)) return invalid(QrError.MALFORMED, "Malformed field name")
            if (value.any { it !in ALLOWED_VALUE_CHARS }) return invalid(QrError.MALFORMED, "Illegal character in '$key'")
            if (fields.put(key, value) != null) return invalid(QrError.DUPLICATE_FIELD, "Field '$key' appears twice")
        }

        val versionText = fields["v"] ?: return invalid(QrError.MISSING_FIELD, "Missing 'v'")
        if (!VERSION_PATTERN.matches(versionText)) return invalid(QrError.MALFORMED, "Malformed version")
        val version = versionText.toInt()
        if (version != ProtocolConstants.PROTOCOL_VERSION) {
            return invalid(QrError.UNSUPPORTED_VERSION, "Pairing code version $version is not supported")
        }

        fields.keys.firstOrNull { it !in FIELDS }?.let { return invalid(QrError.UNKNOWN_FIELD, "Unknown field '$it'") }
        FIELDS.firstOrNull { it !in fields }?.let { return invalid(QrError.MISSING_FIELD, "Missing '$it'") }

        val deviceId = Uuids.parseCanonical(fields.getValue("id"))
            ?: return invalid(QrError.INVALID_DEVICE_ID, "Device ID is not a UUID")
        val advertisingId = Uuids.parseCanonical(fields.getValue("adv"))
            ?: return invalid(QrError.INVALID_ADVERTISING_ID, "Advertising ID is not a UUID")
        val nameBytes = PercentCodec.decode(fields.getValue("name"))
            ?: return invalid(QrError.INVALID_DEVICE_NAME, "Device name has a bad percent-escape")
        val name = DeviceNames.decodeWire(nameBytes)
            ?: return invalid(QrError.INVALID_DEVICE_NAME, "Device name is empty, too long or not valid UTF-8")

        val keyText = fields.getValue("pk")
        if (keyText.length != BASE64URL_KEY_LENGTH) return invalid(QrError.INVALID_PUBLIC_KEY, "Public key has the wrong length")
        val publicKey = Base64Codec.decodeUrlNoPadding(keyText)
            ?: return invalid(QrError.INVALID_PUBLIC_KEY, "Public key is not canonical base64url")
        if (publicKey.size != ProtocolConstants.ED25519_PUBLIC_KEY_SIZE) {
            return invalid(QrError.INVALID_PUBLIC_KEY, "Public key is not 32 bytes")
        }

        return QrParseResult.Valid(QrPairingPayload(version, deviceId, name, advertisingId, publicKey))
    }

    private fun invalid(error: QrError, detail: String) = QrParseResult.Invalid(error, detail)
}

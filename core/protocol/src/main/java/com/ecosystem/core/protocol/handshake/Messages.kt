package com.ecosystem.core.protocol.handshake

import com.ecosystem.core.protocol.ProtocolConstants.DEVICE_ID_SIZE
import com.ecosystem.core.protocol.ProtocolConstants.ED25519_PUBLIC_KEY_SIZE
import com.ecosystem.core.protocol.ProtocolConstants.ED25519_SIGNATURE_SIZE
import com.ecosystem.core.protocol.ProtocolConstants.HMAC_SHA256_SIZE
import com.ecosystem.core.protocol.ProtocolConstants.MAX_DEVICE_NAME_BYTES
import com.ecosystem.core.protocol.ProtocolConstants.NONCE_SIZE
import com.ecosystem.core.protocol.ProtocolConstants.X25519_KEY_SIZE
import com.ecosystem.core.protocol.framing.Frame
import com.ecosystem.core.protocol.tlv.StrictTlvReader
import com.ecosystem.core.protocol.tlv.TlvBuilder
import com.ecosystem.core.protocol.tlv.TlvFormatException
import com.ecosystem.core.protocol.util.DeviceNames

/** TLV tags. `0x0_` common, `0x1_` Android-originated, `0x2_` Mac-originated. */
object ProtocolTags {
    const val PROTOCOL_LABEL = 0x01
    const val PROTOCOL_VERSION = 0x02
    const val PURPOSE = 0x03
    const val TRANSCRIPT_HASH = 0x04
    const val SENDER_METADATA = 0x05

    const val ANDROID_EPHEMERAL_KEY = 0x10
    const val ANDROID_NONCE = 0x11
    const val ANDROID_DEVICE_ID = 0x12
    const val ANDROID_IDENTITY_KEY = 0x13
    const val ANDROID_DEVICE_NAME = 0x14
    const val ANDROID_IP_ADDRESS = 0x15
    const val ANDROID_SIGNATURE = 0x16
    const val ANDROID_AUTH_MAC = 0x17
    const val ANDROID_CONFIRM_MAC = 0x18

    const val MAC_EPHEMERAL_KEY = 0x20
    const val MAC_NONCE = 0x21
    const val MAC_DEVICE_ID = 0x22
    const val MAC_IDENTITY_KEY = 0x23
    const val MAC_IP_ADDRESS = 0x25
    const val MAC_SIGNATURE = 0x26
    const val MAC_AUTH_MAC = 0x27
    const val MAC_COMPLETE_MAC = 0x28

    const val ERROR_CODE = 0x7E
}

enum class MessageType(val code: Int) {
    KEY_EXCHANGE_INIT(0x01),
    KEY_EXCHANGE_RESPONSE(0x02),
    ANDROID_AUTH(0x03),
    MAC_AUTH(0x04),
    ANDROID_CONFIRM(0x05),
    MAC_COMPLETE(0x06),
    ERROR(0x7F);

    companion object {
        fun fromCode(code: Int): MessageType? = entries.firstOrNull { it.code == code }
    }
}

enum class ErrorCode(val code: Int) {
    MALFORMED_MESSAGE(0x01),
    UNSUPPORTED_VERSION(0x02),
    AUTHENTICATION_FAILED(0x03),
    BUSY(0x04),
    CANCELLED(0x05),
    INTERNAL_ERROR(0x06),
    NOT_PAIRING(0x07);

    companion object {
        fun fromCode(code: Int): ErrorCode? = entries.firstOrNull { it.code == code }
    }
}

private val IP_ADDRESS_SIZES = setOf(4, 16)

sealed class PairingMessage(val type: MessageType) {
    abstract fun encodePayload(): ByteArray

    fun toFrame(): Frame = Frame(type.code, encodePayload())
}

/** M1, Android → Mac. */
class KeyExchangeInit(val ephemeralPublicKey: ByteArray, val nonce: ByteArray) :
    PairingMessage(MessageType.KEY_EXCHANGE_INIT) {
    init {
        require(ephemeralPublicKey.size == X25519_KEY_SIZE)
        require(nonce.size == NONCE_SIZE)
    }

    override fun encodePayload(): ByteArray = TlvBuilder()
        .add(ProtocolTags.ANDROID_EPHEMERAL_KEY, ephemeralPublicKey)
        .add(ProtocolTags.ANDROID_NONCE, nonce)
        .build()
}

/** M2, Mac → Android. */
class KeyExchangeResponse(val ephemeralPublicKey: ByteArray, val nonce: ByteArray) :
    PairingMessage(MessageType.KEY_EXCHANGE_RESPONSE) {
    init {
        require(ephemeralPublicKey.size == X25519_KEY_SIZE)
        require(nonce.size == NONCE_SIZE)
    }

    override fun encodePayload(): ByteArray = TlvBuilder()
        .add(ProtocolTags.MAC_EPHEMERAL_KEY, ephemeralPublicKey)
        .add(ProtocolTags.MAC_NONCE, nonce)
        .build()
}

/** M3, Android → Mac. Name and optional IP are "sender metadata" and are covered by the signature and MAC. */
class AndroidAuth(
    val deviceId: ByteArray,
    val identityPublicKey: ByteArray,
    val deviceNameBytes: ByteArray,
    val ipAddress: ByteArray?,
    val signature: ByteArray,
    val authMac: ByteArray,
) : PairingMessage(MessageType.ANDROID_AUTH) {
    val deviceName: String = requireNotNull(DeviceNames.decodeWire(deviceNameBytes)) { "Invalid device name" }

    init {
        require(deviceId.size == DEVICE_ID_SIZE)
        require(identityPublicKey.size == ED25519_PUBLIC_KEY_SIZE)
        require(ipAddress == null || ipAddress.size in IP_ADDRESS_SIZES)
        require(signature.size == ED25519_SIGNATURE_SIZE)
        require(authMac.size == HMAC_SHA256_SIZE)
    }

    fun metadata(): ByteArray = metadataFor(deviceNameBytes, ipAddress)

    override fun encodePayload(): ByteArray = TlvBuilder()
        .add(ProtocolTags.ANDROID_DEVICE_ID, deviceId)
        .add(ProtocolTags.ANDROID_IDENTITY_KEY, identityPublicKey)
        .add(ProtocolTags.ANDROID_DEVICE_NAME, deviceNameBytes)
        .addIfPresent(ProtocolTags.ANDROID_IP_ADDRESS, ipAddress)
        .add(ProtocolTags.ANDROID_SIGNATURE, signature)
        .add(ProtocolTags.ANDROID_AUTH_MAC, authMac)
        .build()

    companion object {
        /** The exact TLV bytes of the name and optional IP fields, as they appear in the message. */
        fun metadataFor(deviceNameBytes: ByteArray, ipAddress: ByteArray?): ByteArray = TlvBuilder()
            .add(ProtocolTags.ANDROID_DEVICE_NAME, deviceNameBytes)
            .addIfPresent(ProtocolTags.ANDROID_IP_ADDRESS, ipAddress)
            .build()
    }
}

/** M4, Mac → Android. The optional IP is sender metadata covered by the signature and MAC. */
class MacAuth(
    val deviceId: ByteArray,
    val ipAddress: ByteArray?,
    val signature: ByteArray,
    val authMac: ByteArray,
) : PairingMessage(MessageType.MAC_AUTH) {
    init {
        require(deviceId.size == DEVICE_ID_SIZE)
        require(ipAddress == null || ipAddress.size in IP_ADDRESS_SIZES)
        require(signature.size == ED25519_SIGNATURE_SIZE)
        require(authMac.size == HMAC_SHA256_SIZE)
    }

    fun metadata(): ByteArray = metadataFor(ipAddress)

    override fun encodePayload(): ByteArray = TlvBuilder()
        .add(ProtocolTags.MAC_DEVICE_ID, deviceId)
        .addIfPresent(ProtocolTags.MAC_IP_ADDRESS, ipAddress)
        .add(ProtocolTags.MAC_SIGNATURE, signature)
        .add(ProtocolTags.MAC_AUTH_MAC, authMac)
        .build()

    companion object {
        fun metadataFor(ipAddress: ByteArray?): ByteArray = TlvBuilder()
            .addIfPresent(ProtocolTags.MAC_IP_ADDRESS, ipAddress)
            .build()
    }
}

/** M5, Android → Mac: Android has verified the Mac. */
class AndroidConfirm(val confirmMac: ByteArray) : PairingMessage(MessageType.ANDROID_CONFIRM) {
    init {
        require(confirmMac.size == HMAC_SHA256_SIZE)
    }

    override fun encodePayload(): ByteArray =
        TlvBuilder().add(ProtocolTags.ANDROID_CONFIRM_MAC, confirmMac).build()
}

/** M6, Mac → Android: the Mac has stored the trust relationship. */
class MacComplete(val completeMac: ByteArray) : PairingMessage(MessageType.MAC_COMPLETE) {
    init {
        require(completeMac.size == HMAC_SHA256_SIZE)
    }

    override fun encodePayload(): ByteArray =
        TlvBuilder().add(ProtocolTags.MAC_COMPLETE_MAC, completeMac).build()
}

/** Either side may send this before disconnecting. It carries no detail beyond the code. */
class ErrorMessage(val code: Int) : PairingMessage(MessageType.ERROR) {
    constructor(errorCode: ErrorCode) : this(errorCode.code)

    init {
        require(code in 0..0xFF)
    }

    val errorCode: ErrorCode? get() = ErrorCode.fromCode(code)

    override fun encodePayload(): ByteArray =
        TlvBuilder().add(ProtocolTags.ERROR_CODE, byteArrayOf(code.toByte())).build()
}

class MalformedMessageException(message: String, cause: Throwable? = null) : Exception(message, cause)

object PairingMessageCodec {

    fun decode(frame: Frame): PairingMessage {
        val type = MessageType.fromCode(frame.type)
            ?: throw MalformedMessageException("Unknown message type 0x%02x".format(frame.type))
        return try {
            val reader = StrictTlvReader(frame.payload)
            val message = when (type) {
                MessageType.KEY_EXCHANGE_INIT -> KeyExchangeInit(
                    ephemeralPublicKey = reader.required(ProtocolTags.ANDROID_EPHEMERAL_KEY, X25519_KEY_SIZE),
                    nonce = reader.required(ProtocolTags.ANDROID_NONCE, NONCE_SIZE),
                )
                MessageType.KEY_EXCHANGE_RESPONSE -> KeyExchangeResponse(
                    ephemeralPublicKey = reader.required(ProtocolTags.MAC_EPHEMERAL_KEY, X25519_KEY_SIZE),
                    nonce = reader.required(ProtocolTags.MAC_NONCE, NONCE_SIZE),
                )
                MessageType.ANDROID_AUTH -> AndroidAuth(
                    deviceId = reader.required(ProtocolTags.ANDROID_DEVICE_ID, DEVICE_ID_SIZE),
                    identityPublicKey = reader.required(ProtocolTags.ANDROID_IDENTITY_KEY, ED25519_PUBLIC_KEY_SIZE),
                    deviceNameBytes = reader.required(ProtocolTags.ANDROID_DEVICE_NAME, 1..MAX_DEVICE_NAME_BYTES),
                    ipAddress = reader.optional(ProtocolTags.ANDROID_IP_ADDRESS, IP_ADDRESS_SIZES),
                    signature = reader.required(ProtocolTags.ANDROID_SIGNATURE, ED25519_SIGNATURE_SIZE),
                    authMac = reader.required(ProtocolTags.ANDROID_AUTH_MAC, HMAC_SHA256_SIZE),
                )
                MessageType.MAC_AUTH -> MacAuth(
                    deviceId = reader.required(ProtocolTags.MAC_DEVICE_ID, DEVICE_ID_SIZE),
                    ipAddress = reader.optional(ProtocolTags.MAC_IP_ADDRESS, IP_ADDRESS_SIZES),
                    signature = reader.required(ProtocolTags.MAC_SIGNATURE, ED25519_SIGNATURE_SIZE),
                    authMac = reader.required(ProtocolTags.MAC_AUTH_MAC, HMAC_SHA256_SIZE),
                )
                MessageType.ANDROID_CONFIRM -> AndroidConfirm(
                    reader.required(ProtocolTags.ANDROID_CONFIRM_MAC, HMAC_SHA256_SIZE)
                )
                MessageType.MAC_COMPLETE -> MacComplete(
                    reader.required(ProtocolTags.MAC_COMPLETE_MAC, HMAC_SHA256_SIZE)
                )
                MessageType.ERROR -> ErrorMessage(
                    reader.required(ProtocolTags.ERROR_CODE, 1)[0].toInt() and 0xFF
                )
            }
            reader.finish()
            message
        } catch (e: TlvFormatException) {
            throw MalformedMessageException("Malformed ${type.name}: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw MalformedMessageException("Malformed ${type.name}: ${e.message}", e)
        }
    }
}

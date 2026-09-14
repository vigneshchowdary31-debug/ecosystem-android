package com.ecosystem.core.protocol.handshake

import com.ecosystem.core.protocol.ProtocolConstants
import com.ecosystem.core.protocol.ProtocolRoles
import com.ecosystem.core.protocol.tlv.TlvBuilder

/**
 * Deterministic handshake transcript. Every field is TLV-encoded (`tag || u16 length || value`)
 * in a fixed order, so the byte string is unambiguous and identical on both platforms.
 *
 * ```
 * header         = TLV(0x01, "ConnectFlow-Pairing") || TLV(0x02, 0x02)
 * keyExchange    = header || TLV(0x10, ephA) || TLV(0x11, nonceA) || TLV(0x20, ephM) || TLV(0x21, nonceM)
 * transcript     = keyExchange || TLV(0x12, idA) || TLV(0x13, pkA) || TLV(0x22, idM) || TLV(0x23, pkM)
 * TH             = SHA-256(transcript)
 * hkdfInfo       = header || TLV(0x03, "pairing-mac-key") || TLV(0x04, SHA-256(keyExchange))
 * authInput(p,m) = header || TLV(0x03, p) || TLV(0x04, TH) || TLV(0x05, m)
 * ```
 */
object Transcript {
    private val LABEL_BYTES = ProtocolConstants.PROTOCOL_LABEL.toByteArray(Charsets.US_ASCII)

    fun header(): ByteArray = TlvBuilder()
        .add(ProtocolTags.PROTOCOL_LABEL, LABEL_BYTES)
        .add(ProtocolTags.PROTOCOL_VERSION, byteArrayOf(ProtocolConstants.PROTOCOL_VERSION.toByte()))
        .build()

    fun keyExchangePrefix(
        androidEphemeralKey: ByteArray,
        androidNonce: ByteArray,
        macEphemeralKey: ByteArray,
        macNonce: ByteArray,
    ): ByteArray = header() + TlvBuilder()
        .add(ProtocolTags.ANDROID_EPHEMERAL_KEY, androidEphemeralKey)
        .add(ProtocolTags.ANDROID_NONCE, androidNonce)
        .add(ProtocolTags.MAC_EPHEMERAL_KEY, macEphemeralKey)
        .add(ProtocolTags.MAC_NONCE, macNonce)
        .build()

    fun full(inputs: TranscriptInputs): ByteArray = keyExchangePrefix(
        inputs.androidEphemeralKey,
        inputs.androidNonce,
        inputs.macEphemeralKey,
        inputs.macNonce,
    ) + TlvBuilder()
        .add(ProtocolTags.ANDROID_DEVICE_ID, inputs.androidDeviceId)
        .add(ProtocolTags.ANDROID_IDENTITY_KEY, inputs.androidIdentityKey)
        .add(ProtocolTags.MAC_DEVICE_ID, inputs.macDeviceId)
        .add(ProtocolTags.MAC_IDENTITY_KEY, inputs.macIdentityKey)
        .build()

    fun hkdfInfo(keyExchangeHash: ByteArray): ByteArray = header() + TlvBuilder()
        .add(ProtocolTags.PURPOSE, ProtocolRoles.MAC_KEY_PURPOSE.toByteArray(Charsets.US_ASCII))
        .add(ProtocolTags.TRANSCRIPT_HASH, keyExchangeHash)
        .build()

    fun authenticationInput(purpose: String, transcriptHash: ByteArray, senderMetadata: ByteArray): ByteArray =
        header() + TlvBuilder()
            .add(ProtocolTags.PURPOSE, purpose.toByteArray(Charsets.US_ASCII))
            .add(ProtocolTags.TRANSCRIPT_HASH, transcriptHash)
            .add(ProtocolTags.SENDER_METADATA, senderMetadata)
            .build()
}

class TranscriptInputs(
    val androidEphemeralKey: ByteArray,
    val androidNonce: ByteArray,
    val macEphemeralKey: ByteArray,
    val macNonce: ByteArray,
    val androidDeviceId: ByteArray,
    val androidIdentityKey: ByteArray,
    val macDeviceId: ByteArray,
    val macIdentityKey: ByteArray,
) {
    init {
        require(androidEphemeralKey.size == ProtocolConstants.X25519_KEY_SIZE)
        require(androidNonce.size == ProtocolConstants.NONCE_SIZE)
        require(macEphemeralKey.size == ProtocolConstants.X25519_KEY_SIZE)
        require(macNonce.size == ProtocolConstants.NONCE_SIZE)
        require(androidDeviceId.size == ProtocolConstants.DEVICE_ID_SIZE)
        require(androidIdentityKey.size == ProtocolConstants.ED25519_PUBLIC_KEY_SIZE)
        require(macDeviceId.size == ProtocolConstants.DEVICE_ID_SIZE)
        require(macIdentityKey.size == ProtocolConstants.ED25519_PUBLIC_KEY_SIZE)
    }
}

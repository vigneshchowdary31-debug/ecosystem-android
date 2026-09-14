package com.ecosystem.core.protocol

import com.ecosystem.core.protocol.crypto.Ed25519SeedSigner
import com.ecosystem.core.protocol.crypto.TinkHandshakeCrypto
import com.ecosystem.core.protocol.framing.FrameCodec
import com.ecosystem.core.protocol.handshake.AndroidAuth
import com.ecosystem.core.protocol.handshake.AndroidConfirm
import com.ecosystem.core.protocol.handshake.ErrorCode
import com.ecosystem.core.protocol.handshake.ErrorMessage
import com.ecosystem.core.protocol.handshake.ExpectedPeer
import com.ecosystem.core.protocol.handshake.InitiatorHandshake
import com.ecosystem.core.protocol.handshake.KeyExchangeInit
import com.ecosystem.core.protocol.handshake.KeyExchangeResponse
import com.ecosystem.core.protocol.handshake.LocalPairingIdentity
import com.ecosystem.core.protocol.handshake.MacAuth
import com.ecosystem.core.protocol.handshake.MacComplete
import com.ecosystem.core.protocol.handshake.Transcript
import com.ecosystem.core.protocol.handshake.TranscriptInputs
import com.ecosystem.core.protocol.qr.QrPairingPayload
import com.ecosystem.core.protocol.qr.QrPayloadCodec
import com.ecosystem.core.protocol.testing.InMemoryLink
import com.ecosystem.core.protocol.testing.ResponderHandshake
import com.ecosystem.core.protocol.testing.ScriptedSecureRandom
import com.ecosystem.core.protocol.util.Base64Codec
import com.ecosystem.core.protocol.util.DeviceNames
import com.ecosystem.core.protocol.util.Uuids
import com.ecosystem.core.protocol.util.toHex
import com.google.crypto.tink.subtle.X25519
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Cross-platform test vectors for protocol v2. The checked-in JSON is also published in
 * `ecosystem-shared-docs/protocol/test-vectors/` where a Swift/CryptoKit script verifies it,
 * so Android and macOS are held to the same bytes.
 *
 * Regenerate after an intentional protocol change with:
 * `WRITE_VECTORS=/path/to/pairing-v2-vectors.json ./gradlew :core:protocol:test`
 */
class ProtocolVectorsTest {

    @Test
    fun `vectors match the checked-in file`() {
        val json = ProtocolVectors.toJson(ProtocolVectors.generate())
        System.getenv("WRITE_VECTORS")?.let { File(it).writeText(json) }
        val checkedIn = javaClass.getResource("/pairing-v2-vectors.json")?.readText()
            ?: error("pairing-v2-vectors.json missing from test resources")
        assertEquals(checkedIn, json)
    }

    @Test
    fun `a real handshake with the vector inputs produces the vector frames`() = runBlocking {
        val v = ProtocolVectors.generate()
        val inputs = ProtocolVectors.Inputs
        val androidCrypto = TinkHandshakeCrypto(ScriptedSecureRandom(listOf(inputs.androidEphemeralPrivate, inputs.androidNonce)))
        val macCrypto = TinkHandshakeCrypto(ScriptedSecureRandom(listOf(inputs.macEphemeralPrivate, inputs.macNonce)))
        val (androidEnd, macEnd) = InMemoryLink.pair(chunkSize = 20)
        coroutineScope {
            val mac = async { ResponderHandshake(macCrypto, inputs.macIdentity, inputs.macIp).run(macEnd) }
            InitiatorHandshake(androidCrypto, inputs.androidIdentity, inputs.expectedMac, inputs.androidIp).run(androidEnd)
            mac.await()
        }
        val androidFrames = androidEnd.sentFrames.map { FrameCodec.encode(it).toHex() }
        val macFrames = macEnd.sentFrames.map { FrameCodec.encode(it).toHex() }
        assertEquals(listOf(v["frame_m1"], v["frame_m3"], v["frame_m5"]), androidFrames)
        assertEquals(listOf(v["frame_m2"], v["frame_m4"], v["frame_m6"]), macFrames)
    }
}

object ProtocolVectors {

    private fun sha256(text: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.US_ASCII))

    private fun clamped(bytes: ByteArray): ByteArray = bytes.copyOf().also { TinkHandshakeCrypto.clampX25519(it) }

    object Inputs {
        val androidIdentitySeed = sha256("ConnectFlow v2 test vector: android identity seed")
        val macIdentitySeed = sha256("ConnectFlow v2 test vector: mac identity seed")
        val androidEphemeralPrivate = clamped(sha256("ConnectFlow v2 test vector: android ephemeral"))
        val macEphemeralPrivate = clamped(sha256("ConnectFlow v2 test vector: mac ephemeral"))
        val androidNonce = sha256("ConnectFlow v2 test vector: android nonce")
        val macNonce = sha256("ConnectFlow v2 test vector: mac nonce")
        val androidDeviceId: UUID = UUID.fromString("5f1c2b7a-3d4e-4f60-8a9b-0c1d2e3f4a5b")
        val macDeviceId: UUID = UUID.fromString("2d861d8a-6b83-4a1d-8f9c-76e48c3f8f11")
        val macAdvertisingId: UUID = UUID.fromString("f7823ab4-3e9a-4c28-be51-24b893a201f9")
        const val ANDROID_NAME = "Pixel 9 Pro"
        const val MAC_NAME = "Vignesh’s MacBook Pro, 14\""
        val androidIp = byteArrayOf(192.toByte(), 168.toByte(), 1, 23)
        val macIp = byteArrayOf(192.toByte(), 168.toByte(), 1, 10)

        val androidSigner = Ed25519SeedSigner(androidIdentitySeed)
        val macSigner = Ed25519SeedSigner(macIdentitySeed)
        val androidIdentity = LocalPairingIdentity(androidDeviceId, androidSigner.publicKey, ANDROID_NAME, androidSigner)
        val macIdentity = LocalPairingIdentity(macDeviceId, macSigner.publicKey, MAC_NAME, macSigner)
        val expectedMac = ExpectedPeer(macDeviceId, macSigner.publicKey, MAC_NAME)
    }

    fun generate(): LinkedHashMap<String, String> = runBlocking {
        val i = Inputs
        val crypto = TinkHandshakeCrypto()
        val v = LinkedHashMap<String, String>()

        val androidEphemeralPublic = X25519.publicFromPrivate(i.androidEphemeralPrivate)
        val macEphemeralPublic = X25519.publicFromPrivate(i.macEphemeralPrivate)
        val androidIdBytes = Uuids.toBytes(i.androidDeviceId)
        val macIdBytes = Uuids.toBytes(i.macDeviceId)

        v["protocol_version"] = ProtocolConstants.PROTOCOL_VERSION.toString()
        v["protocol_label"] = ProtocolConstants.PROTOCOL_LABEL
        v["android_identity_seed"] = i.androidIdentitySeed.toHex()
        v["android_identity_public_key"] = i.androidSigner.publicKey.toHex()
        v["mac_identity_seed"] = i.macIdentitySeed.toHex()
        v["mac_identity_public_key"] = i.macSigner.publicKey.toHex()
        v["android_device_id"] = Uuids.canonical(i.androidDeviceId)
        v["android_device_id_bytes"] = androidIdBytes.toHex()
        v["mac_device_id"] = Uuids.canonical(i.macDeviceId)
        v["mac_device_id_bytes"] = macIdBytes.toHex()
        v["android_device_name"] = i.ANDROID_NAME
        v["android_device_name_bytes"] = DeviceNames.toWireBytes(i.ANDROID_NAME).toHex()
        v["android_ip_address"] = i.androidIp.toHex()
        v["mac_ip_address"] = i.macIp.toHex()
        v["android_ephemeral_private_key"] = i.androidEphemeralPrivate.toHex()
        v["android_ephemeral_public_key"] = androidEphemeralPublic.toHex()
        v["android_nonce"] = i.androidNonce.toHex()
        v["mac_ephemeral_private_key"] = i.macEphemeralPrivate.toHex()
        v["mac_ephemeral_public_key"] = macEphemeralPublic.toHex()
        v["mac_nonce"] = i.macNonce.toHex()

        val sharedSecret = crypto.x25519(i.androidEphemeralPrivate, macEphemeralPublic)
        check(sharedSecret.contentEquals(crypto.x25519(i.macEphemeralPrivate, androidEphemeralPublic)))
        v["shared_secret"] = sharedSecret.toHex()

        val keyExchange = Transcript.keyExchangePrefix(androidEphemeralPublic, i.androidNonce, macEphemeralPublic, i.macNonce)
        val keyExchangeHash = crypto.sha256(keyExchange)
        val hkdfInfo = Transcript.hkdfInfo(keyExchangeHash)
        val sessionKey = crypto.hkdfSha256(sharedSecret, ByteArray(0), hkdfInfo, ProtocolConstants.SESSION_KEY_SIZE)
        v["key_exchange_transcript"] = keyExchange.toHex()
        v["key_exchange_hash"] = keyExchangeHash.toHex()
        v["hkdf_salt"] = ""
        v["hkdf_info"] = hkdfInfo.toHex()
        v["session_mac_key"] = sessionKey.toHex()

        val transcript = Transcript.full(
            TranscriptInputs(
                androidEphemeralPublic, i.androidNonce, macEphemeralPublic, i.macNonce,
                androidIdBytes, i.androidSigner.publicKey, macIdBytes, i.macSigner.publicKey,
            )
        )
        val transcriptHash = crypto.sha256(transcript)
        v["transcript"] = transcript.toHex()
        v["transcript_hash"] = transcriptHash.toHex()

        val nameBytes = DeviceNames.toWireBytes(i.ANDROID_NAME)
        val androidMetadata = AndroidAuth.metadataFor(nameBytes, i.androidIp)
        val androidInput = Transcript.authenticationInput(ProtocolRoles.ANDROID_AUTH, transcriptHash, androidMetadata)
        val androidSignature = i.androidSigner.sign(androidInput)
        val androidMac = crypto.hmacSha256(sessionKey, androidInput)
        v["android_auth_metadata"] = androidMetadata.toHex()
        v["android_auth_input"] = androidInput.toHex()
        v["android_auth_signature"] = androidSignature.toHex()
        v["android_auth_mac"] = androidMac.toHex()

        val macMetadata = MacAuth.metadataFor(i.macIp)
        val macInput = Transcript.authenticationInput(ProtocolRoles.MAC_AUTH, transcriptHash, macMetadata)
        val macSignature = i.macSigner.sign(macInput)
        val macMac = crypto.hmacSha256(sessionKey, macInput)
        v["mac_auth_metadata"] = macMetadata.toHex()
        v["mac_auth_input"] = macInput.toHex()
        v["mac_auth_signature"] = macSignature.toHex()
        v["mac_auth_mac"] = macMac.toHex()

        val confirmInput = Transcript.authenticationInput(ProtocolRoles.ANDROID_CONFIRM, transcriptHash, ByteArray(0))
        val confirmMac = crypto.hmacSha256(sessionKey, confirmInput)
        val completeInput = Transcript.authenticationInput(ProtocolRoles.MAC_COMPLETE, transcriptHash, ByteArray(0))
        val completeMac = crypto.hmacSha256(sessionKey, completeInput)
        v["android_confirm_input"] = confirmInput.toHex()
        v["android_confirm_mac"] = confirmMac.toHex()
        v["mac_complete_input"] = completeInput.toHex()
        v["mac_complete_mac"] = completeMac.toHex()

        v["frame_m1"] = FrameCodec.encode(KeyExchangeInit(androidEphemeralPublic, i.androidNonce).toFrame()).toHex()
        v["frame_m2"] = FrameCodec.encode(KeyExchangeResponse(macEphemeralPublic, i.macNonce).toFrame()).toHex()
        v["frame_m3"] = FrameCodec.encode(
            AndroidAuth(androidIdBytes, i.androidSigner.publicKey, nameBytes, i.androidIp, androidSignature, androidMac).toFrame()
        ).toHex()
        v["frame_m4"] = FrameCodec.encode(MacAuth(macIdBytes, i.macIp, macSignature, macMac).toFrame()).toHex()
        v["frame_m5"] = FrameCodec.encode(AndroidConfirm(confirmMac).toFrame()).toHex()
        v["frame_m6"] = FrameCodec.encode(MacComplete(completeMac).toFrame()).toHex()
        v["frame_error_authentication_failed"] = FrameCodec.encode(ErrorMessage(ErrorCode.AUTHENTICATION_FAILED).toFrame()).toHex()

        v["crc32_input_ascii"] = "123456789"
        v["crc32_expected"] = "cbf43926"

        v["qr_mac_device_name"] = i.MAC_NAME
        v["qr_advertising_id"] = Uuids.canonical(i.macAdvertisingId)
        v["qr_public_key_base64url"] = Base64Codec.encodeUrlNoPadding(i.macSigner.publicKey)
        v["qr_payload"] = QrPayloadCodec.encode(
            QrPairingPayload(ProtocolConstants.PROTOCOL_VERSION, i.macDeviceId, i.MAC_NAME, i.macAdvertisingId, i.macSigner.publicKey)
        )
        v
    }

    fun toJson(values: Map<String, String>): String {
        val body = values.entries.joinToString(",\n") { (key, value) -> "  \"$key\": \"${escape(value)}\"" }
        return "{\n$body\n}\n"
    }

    private fun escape(text: String): String = buildString {
        for (c in text) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c.code < 0x20 || c.code > 0x7E -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
    }
}

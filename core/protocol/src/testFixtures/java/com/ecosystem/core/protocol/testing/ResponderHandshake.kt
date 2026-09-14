package com.ecosystem.core.protocol.testing

import com.ecosystem.core.protocol.ProtocolConstants
import com.ecosystem.core.protocol.ProtocolRoles
import com.ecosystem.core.protocol.crypto.HandshakeCrypto
import com.ecosystem.core.protocol.crypto.InvalidPeerKeyException
import com.ecosystem.core.protocol.handshake.AndroidAuth
import com.ecosystem.core.protocol.handshake.AndroidConfirm
import com.ecosystem.core.protocol.handshake.ErrorCode
import com.ecosystem.core.protocol.handshake.ErrorMessage
import com.ecosystem.core.protocol.handshake.HandshakeException
import com.ecosystem.core.protocol.handshake.HandshakeTimeouts
import com.ecosystem.core.protocol.handshake.KeyExchangeInit
import com.ecosystem.core.protocol.handshake.KeyExchangeResponse
import com.ecosystem.core.protocol.handshake.LocalPairingIdentity
import com.ecosystem.core.protocol.handshake.MacAuth
import com.ecosystem.core.protocol.handshake.MacComplete
import com.ecosystem.core.protocol.handshake.MalformedMessageException
import com.ecosystem.core.protocol.handshake.PairingMessage
import com.ecosystem.core.protocol.handshake.PairingMessageCodec
import com.ecosystem.core.protocol.handshake.Transcript
import com.ecosystem.core.protocol.handshake.TranscriptInputs
import com.ecosystem.core.protocol.link.LinkException
import com.ecosystem.core.protocol.link.MessageChannel
import com.ecosystem.core.protocol.util.Uuids
import com.ecosystem.core.protocol.util.constantTimeEquals
import com.ecosystem.core.protocol.util.wipe
import java.util.UUID

class ResponderResult(
    val androidDeviceId: UUID,
    val androidIdentityPublicKey: ByteArray,
    val androidDeviceName: String,
    val androidIpAddress: ByteArray?,
    val transcriptHash: ByteArray,
)

/**
 * Reference implementation of the macOS (responder) role, written from the protocol spec.
 * The Android app never runs it. It is the fake Mac in tests and the source of the
 * cross-platform test vectors.
 *
 * Note the trust gap this makes visible: the Android identity key arrives inside ANDROID_AUTH
 * and nothing out of band vouches for it, so the Mac can only verify that the key is bound to
 * this session, not that it belongs to the phone the user intended.
 */
class ResponderHandshake(
    private val crypto: HandshakeCrypto,
    private val local: LocalPairingIdentity,
    private val localIpAddress: ByteArray? = null,
    private val timeouts: HandshakeTimeouts = HandshakeTimeouts(),
    /**
     * False simulates an attacker that answers regardless of what Android sent, so tests can
     * check that Android's own verification rejects it (an honest Mac would stop earlier).
     */
    private val verifyAndroidAuthentication: Boolean = true,
) {
    suspend fun run(channel: MessageChannel): ResponderResult {
        val init = receive<KeyExchangeInit>(channel, timeouts.keyExchangeMillis)
        val keyPair = crypto.generateX25519KeyPair()
        val macNonce = crypto.randomBytes(ProtocolConstants.NONCE_SIZE)
        channel.send(KeyExchangeResponse(keyPair.publicKey, macNonce).toFrame())

        val sharedSecret = try {
            crypto.x25519(keyPair.privateKey, init.ephemeralPublicKey)
        } catch (e: InvalidPeerKeyException) {
            fail(channel, HandshakeException.Reason.INVALID_PEER_KEY, "Android ephemeral key rejected")
        } finally {
            keyPair.wipePrivateKey()
        }
        val keyExchangePrefix = Transcript.keyExchangePrefix(
            init.ephemeralPublicKey, init.nonce, keyPair.publicKey, macNonce
        )
        val key = crypto.hkdfSha256(
            sharedSecret, ByteArray(0), Transcript.hkdfInfo(crypto.sha256(keyExchangePrefix)), ProtocolConstants.SESSION_KEY_SIZE
        )
        sharedSecret.wipe()

        try {
            val auth = receive<AndroidAuth>(channel, timeouts.authenticationMillis)
            val transcriptHash = crypto.sha256(
                Transcript.full(
                    TranscriptInputs(
                        androidEphemeralKey = init.ephemeralPublicKey,
                        androidNonce = init.nonce,
                        macEphemeralKey = keyPair.publicKey,
                        macNonce = macNonce,
                        androidDeviceId = auth.deviceId,
                        androidIdentityKey = auth.identityPublicKey,
                        macDeviceId = Uuids.toBytes(local.deviceId),
                        macIdentityKey = local.identityPublicKey,
                    )
                )
            )
            val androidInput = Transcript.authenticationInput(ProtocolRoles.ANDROID_AUTH, transcriptHash, auth.metadata())
            if (verifyAndroidAuthentication) {
                if (!crypto.verifyEd25519(auth.identityPublicKey, androidInput, auth.signature)) {
                    fail(channel, HandshakeException.Reason.INVALID_SIGNATURE, "Android signature invalid")
                }
                if (!constantTimeEquals(crypto.hmacSha256(key, androidInput), auth.authMac)) {
                    fail(channel, HandshakeException.Reason.INVALID_MAC, "Android MAC invalid")
                }
            }

            val macInput = Transcript.authenticationInput(
                ProtocolRoles.MAC_AUTH, transcriptHash, MacAuth.metadataFor(localIpAddress)
            )
            channel.send(
                MacAuth(
                    deviceId = Uuids.toBytes(local.deviceId),
                    ipAddress = localIpAddress,
                    signature = local.signer.sign(macInput),
                    authMac = crypto.hmacSha256(key, macInput),
                ).toFrame()
            )

            val confirm = receive<AndroidConfirm>(channel, timeouts.completionMillis)
            val confirmInput = Transcript.authenticationInput(ProtocolRoles.ANDROID_CONFIRM, transcriptHash, ByteArray(0))
            if (verifyAndroidAuthentication && !constantTimeEquals(crypto.hmacSha256(key, confirmInput), confirm.confirmMac)) {
                fail(channel, HandshakeException.Reason.INVALID_MAC, "Android confirmation MAC invalid")
            }

            // A real Mac persists the Android device here, before sending MAC_COMPLETE.
            val completeInput = Transcript.authenticationInput(ProtocolRoles.MAC_COMPLETE, transcriptHash, ByteArray(0))
            channel.send(MacComplete(crypto.hmacSha256(key, completeInput)).toFrame())

            return ResponderResult(
                androidDeviceId = Uuids.fromBytes(auth.deviceId),
                androidIdentityPublicKey = auth.identityPublicKey,
                androidDeviceName = auth.deviceName,
                androidIpAddress = auth.ipAddress,
                transcriptHash = transcriptHash,
            )
        } finally {
            key.wipe()
        }
    }

    private suspend fun fail(channel: MessageChannel, reason: HandshakeException.Reason, message: String): Nothing {
        try {
            channel.send(ErrorMessage(ErrorCode.AUTHENTICATION_FAILED).toFrame())
        } catch (ignored: LinkException) {
        }
        throw HandshakeException(reason, message)
    }

    private suspend inline fun <reified T : PairingMessage> receive(channel: MessageChannel, timeoutMillis: Long): T {
        val frame = try {
            channel.receive(timeoutMillis)
        } catch (e: LinkException) {
            throw HandshakeException(HandshakeException.Reason.TRANSPORT_ERROR, "Link failed: ${e.kind}", cause = e)
        }
        val message = try {
            PairingMessageCodec.decode(frame)
        } catch (e: MalformedMessageException) {
            channel.send(ErrorMessage(ErrorCode.MALFORMED_MESSAGE).toFrame())
            throw HandshakeException(HandshakeException.Reason.MALFORMED_MESSAGE, e.message ?: "malformed", cause = e)
        }
        if (message is ErrorMessage) {
            throw HandshakeException(HandshakeException.Reason.PEER_REJECTED, "Android reported ${message.code}", peerErrorCode = message.code)
        }
        return message as? T
            ?: throw HandshakeException(HandshakeException.Reason.UNEXPECTED_MESSAGE, "Unexpected ${message.type}")
    }
}

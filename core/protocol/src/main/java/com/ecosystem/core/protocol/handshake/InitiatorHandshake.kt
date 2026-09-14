package com.ecosystem.core.protocol.handshake

import com.ecosystem.core.protocol.ProtocolConstants
import com.ecosystem.core.protocol.ProtocolRoles
import com.ecosystem.core.protocol.crypto.HandshakeCrypto
import com.ecosystem.core.protocol.crypto.IdentitySigner
import com.ecosystem.core.protocol.crypto.InvalidPeerKeyException
import com.ecosystem.core.protocol.crypto.X25519KeyPair
import com.ecosystem.core.protocol.framing.FramingException
import com.ecosystem.core.protocol.link.LinkException
import com.ecosystem.core.protocol.link.MessageChannel
import com.ecosystem.core.protocol.util.DeviceNames
import com.ecosystem.core.protocol.util.Uuids
import com.ecosystem.core.protocol.util.constantTimeEquals
import com.ecosystem.core.protocol.util.wipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/** This device's long-term identity as the handshake sees it. The private key stays behind [signer]. */
class LocalPairingIdentity(
    val deviceId: UUID,
    val identityPublicKey: ByteArray,
    val deviceName: String,
    val signer: IdentitySigner,
) {
    init {
        require(identityPublicKey.size == ProtocolConstants.ED25519_PUBLIC_KEY_SIZE)
    }
}

/** The peer the user selected by scanning its QR code. This is the trust anchor. */
class ExpectedPeer(val deviceId: UUID, val identityPublicKey: ByteArray, val deviceName: String) {
    init {
        require(identityPublicKey.size == ProtocolConstants.ED25519_PUBLIC_KEY_SIZE)
    }
}

class HandshakeResult(
    val peerDeviceId: UUID,
    val peerIdentityPublicKey: ByteArray,
    val peerDeviceName: String,
    /** Raw 4- or 16-byte address the Mac reported. Authenticated by the Mac's signature and MAC. */
    val peerIpAddress: ByteArray?,
    val transcriptHash: ByteArray,
)

data class HandshakeTimeouts(
    val keyExchangeMillis: Long = 10_000,
    val authenticationMillis: Long = 15_000,
    val completionMillis: Long = 15_000,
    val errorNotificationMillis: Long = 1_000,
)

class HandshakeException(
    val reason: Reason,
    message: String,
    val peerErrorCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Reason(val isAuthenticationFailure: Boolean = false) {
        MALFORMED_MESSAGE,
        UNEXPECTED_MESSAGE,
        PROTOCOL_VERSION_MISMATCH,
        INVALID_PEER_KEY(true),
        REFLECTED_MESSAGE(true),
        WRONG_DEVICE_ID(true),
        INVALID_SIGNATURE(true),
        INVALID_MAC(true),
        PEER_REJECTED,
        TIMEOUT,
        DISCONNECTED,
        TRANSPORT_ERROR,
        LOCAL_IDENTITY_ERROR,
        ALREADY_USED,
    }
}

/**
 * Android (initiator) side of the v2 pairing handshake. One instance runs exactly one session
 * and owns that session's secrets: the ephemeral X25519 private key, the derived MAC key and
 * the transcript hash. [wipe] zeroes them; it runs automatically when [run] finishes.
 *
 * Message order: M1 KEY_EXCHANGE_INIT → M2 KEY_EXCHANGE_RESPONSE → M3 ANDROID_AUTH →
 * M4 MAC_AUTH → M5 ANDROID_CONFIRM → M6 MAC_COMPLETE.
 */
class InitiatorHandshake(
    private val crypto: HandshakeCrypto,
    private val local: LocalPairingIdentity,
    private val expectedPeer: ExpectedPeer,
    private val localIpAddress: ByteArray? = null,
    private val timeouts: HandshakeTimeouts = HandshakeTimeouts(),
    private val listener: Listener = Listener.NONE,
) {
    interface Listener {
        /** M2 received and the session key derived. */
        fun onKeyExchangeComplete() {}

        /** M4 verified: the peer holds the QR identity key and bound it to this session. */
        fun onPeerAuthenticated() {}

        companion object {
            val NONE: Listener = object : Listener {}
        }
    }

    private val lock = Any()
    private var started = false
    private var ephemeral: X25519KeyPair? = null
    private var sessionKey: ByteArray? = null

    /** True while the instance still holds key material. */
    val holdsSecrets: Boolean
        get() = synchronized(lock) { ephemeral != null || sessionKey != null }

    suspend fun run(channel: MessageChannel): HandshakeResult {
        synchronized(lock) {
            if (started) throw HandshakeException(HandshakeException.Reason.ALREADY_USED, "A handshake instance runs once")
            started = true
        }
        try {
            return execute(channel)
        } catch (e: HandshakeException) {
            notifyPeer(channel, e)
            throw e
        } finally {
            wipe()
        }
    }

    /** Zeroes all key material. Safe to call more than once and from any thread. */
    fun wipe() {
        synchronized(lock) {
            ephemeral?.wipePrivateKey()
            ephemeral = null
            sessionKey?.wipe()
            sessionKey = null
        }
    }

    private suspend fun execute(channel: MessageChannel): HandshakeResult {
        val keyPair = crypto.generateX25519KeyPair()
        synchronized(lock) { ephemeral = keyPair }
        val androidNonce = crypto.randomBytes(ProtocolConstants.NONCE_SIZE)
        send(channel, KeyExchangeInit(keyPair.publicKey, androidNonce))

        val response = receive<KeyExchangeResponse>(
            channel, MessageType.KEY_EXCHANGE_RESPONSE, timeouts.keyExchangeMillis
        )
        if (constantTimeEquals(response.ephemeralPublicKey, keyPair.publicKey) ||
            constantTimeEquals(response.nonce, androidNonce)
        ) {
            throw HandshakeException(HandshakeException.Reason.REFLECTED_MESSAGE, "Peer echoed our key-exchange values")
        }

        val sharedSecret = try {
            crypto.x25519(keyPair.privateKey, response.ephemeralPublicKey)
        } catch (e: InvalidPeerKeyException) {
            throw HandshakeException(HandshakeException.Reason.INVALID_PEER_KEY, "Peer ephemeral key rejected", cause = e)
        } finally {
            keyPair.wipePrivateKey()
        }
        val keyExchangePrefix = Transcript.keyExchangePrefix(
            keyPair.publicKey, androidNonce, response.ephemeralPublicKey, response.nonce
        )
        val key = try {
            crypto.hkdfSha256(
                ikm = sharedSecret,
                salt = ByteArray(0),
                info = Transcript.hkdfInfo(crypto.sha256(keyExchangePrefix)),
                length = ProtocolConstants.SESSION_KEY_SIZE,
            )
        } finally {
            sharedSecret.wipe()
        }
        synchronized(lock) { sessionKey = key }
        listener.onKeyExchangeComplete()

        val localDeviceId = Uuids.toBytes(local.deviceId)
        val peerDeviceId = Uuids.toBytes(expectedPeer.deviceId)
        val transcriptHash = crypto.sha256(
            Transcript.full(
                TranscriptInputs(
                    androidEphemeralKey = keyPair.publicKey,
                    androidNonce = androidNonce,
                    macEphemeralKey = response.ephemeralPublicKey,
                    macNonce = response.nonce,
                    androidDeviceId = localDeviceId,
                    androidIdentityKey = local.identityPublicKey,
                    macDeviceId = peerDeviceId,
                    macIdentityKey = expectedPeer.identityPublicKey,
                )
            )
        )

        val nameBytes = DeviceNames.toWireBytes(local.deviceName)
        val androidAuthInput = Transcript.authenticationInput(
            ProtocolRoles.ANDROID_AUTH, transcriptHash, AndroidAuth.metadataFor(nameBytes, localIpAddress)
        )
        val signature = try {
            local.signer.sign(androidAuthInput)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw HandshakeException(HandshakeException.Reason.LOCAL_IDENTITY_ERROR, "Identity signing failed", cause = e)
        }
        if (signature.size != ProtocolConstants.ED25519_SIGNATURE_SIZE) {
            throw HandshakeException(HandshakeException.Reason.LOCAL_IDENTITY_ERROR, "Identity signer returned ${signature.size} bytes")
        }
        send(
            channel,
            AndroidAuth(
                deviceId = localDeviceId,
                identityPublicKey = local.identityPublicKey,
                deviceNameBytes = nameBytes,
                ipAddress = localIpAddress,
                signature = signature,
                authMac = crypto.hmacSha256(key, androidAuthInput),
            )
        )

        val macAuth = receive<MacAuth>(channel, MessageType.MAC_AUTH, timeouts.authenticationMillis)
        if (!constantTimeEquals(macAuth.deviceId, peerDeviceId)) {
            throw HandshakeException(HandshakeException.Reason.WRONG_DEVICE_ID, "Peer device ID does not match the QR code")
        }
        val macAuthInput = Transcript.authenticationInput(ProtocolRoles.MAC_AUTH, transcriptHash, macAuth.metadata())
        if (!crypto.verifyEd25519(expectedPeer.identityPublicKey, macAuthInput, macAuth.signature)) {
            throw HandshakeException(HandshakeException.Reason.INVALID_SIGNATURE, "Peer signature does not verify against the QR identity key")
        }
        if (!constantTimeEquals(crypto.hmacSha256(key, macAuthInput), macAuth.authMac)) {
            throw HandshakeException(HandshakeException.Reason.INVALID_MAC, "Peer authentication MAC is invalid")
        }
        listener.onPeerAuthenticated()

        val confirmInput = Transcript.authenticationInput(ProtocolRoles.ANDROID_CONFIRM, transcriptHash, ByteArray(0))
        send(channel, AndroidConfirm(crypto.hmacSha256(key, confirmInput)))

        val complete = receive<MacComplete>(channel, MessageType.MAC_COMPLETE, timeouts.completionMillis)
        val completeInput = Transcript.authenticationInput(ProtocolRoles.MAC_COMPLETE, transcriptHash, ByteArray(0))
        if (!constantTimeEquals(crypto.hmacSha256(key, completeInput), complete.completeMac)) {
            throw HandshakeException(HandshakeException.Reason.INVALID_MAC, "Peer completion MAC is invalid")
        }

        return HandshakeResult(
            peerDeviceId = expectedPeer.deviceId,
            peerIdentityPublicKey = expectedPeer.identityPublicKey.copyOf(),
            peerDeviceName = expectedPeer.deviceName,
            peerIpAddress = macAuth.ipAddress?.copyOf(),
            transcriptHash = transcriptHash,
        )
    }

    private suspend fun send(channel: MessageChannel, message: PairingMessage) {
        try {
            channel.send(message.toFrame())
        } catch (e: LinkException) {
            throw e.toHandshakeException()
        }
    }

    private suspend inline fun <reified T : PairingMessage> receive(
        channel: MessageChannel,
        expected: MessageType,
        timeoutMillis: Long,
    ): T {
        val frame = try {
            channel.receive(timeoutMillis)
        } catch (e: LinkException) {
            throw e.toHandshakeException()
        }
        val message = try {
            PairingMessageCodec.decode(frame)
        } catch (e: MalformedMessageException) {
            throw HandshakeException(HandshakeException.Reason.MALFORMED_MESSAGE, e.message ?: "Malformed message", cause = e)
        }
        if (message is ErrorMessage) {
            val reason = if (message.errorCode == ErrorCode.UNSUPPORTED_VERSION) {
                HandshakeException.Reason.PROTOCOL_VERSION_MISMATCH
            } else {
                HandshakeException.Reason.PEER_REJECTED
            }
            throw HandshakeException(reason, "Peer reported error 0x%02x".format(message.code), peerErrorCode = message.code)
        }
        return message as? T
            ?: throw HandshakeException(HandshakeException.Reason.UNEXPECTED_MESSAGE, "Expected $expected but received ${message.type}")
    }

    /** Best effort: tell the peer why we are aborting, so it can fail fast instead of timing out. */
    private suspend fun notifyPeer(channel: MessageChannel, failure: HandshakeException) {
        val code = when {
            failure.reason.isAuthenticationFailure -> ErrorCode.AUTHENTICATION_FAILED
            failure.reason == HandshakeException.Reason.MALFORMED_MESSAGE ||
                failure.reason == HandshakeException.Reason.UNEXPECTED_MESSAGE -> ErrorCode.MALFORMED_MESSAGE
            failure.reason == HandshakeException.Reason.LOCAL_IDENTITY_ERROR -> ErrorCode.INTERNAL_ERROR
            else -> return
        }
        withTimeoutOrNull(timeouts.errorNotificationMillis) {
            try {
                channel.send(ErrorMessage(code).toFrame())
            } catch (ignored: LinkException) {
                // The link is already gone; nothing else to do.
            }
        }
    }
}

internal fun LinkException.toHandshakeException(): HandshakeException = when (kind) {
    LinkException.Kind.TIMEOUT -> HandshakeException(HandshakeException.Reason.TIMEOUT, "Timed out waiting for the peer", cause = this)
    LinkException.Kind.DISCONNECTED -> HandshakeException(HandshakeException.Reason.DISCONNECTED, "Peer disconnected", cause = this)
    LinkException.Kind.FRAMING_ERROR -> {
        val framing = cause as? FramingException
        if (framing?.reason == FramingException.Reason.UNSUPPORTED_VERSION) {
            HandshakeException(HandshakeException.Reason.PROTOCOL_VERSION_MISMATCH, "Peer speaks another protocol version", cause = this)
        } else {
            HandshakeException(HandshakeException.Reason.MALFORMED_MESSAGE, "Invalid frame from peer", cause = this)
        }
    }
    LinkException.Kind.WRITE_FAILED, LinkException.Kind.PERMISSION_DENIED ->
        HandshakeException(HandshakeException.Reason.TRANSPORT_ERROR, message ?: "Transport error", cause = this)
}

package com.ecosystem.core.pairing

import com.ecosystem.core.protocol.handshake.HandshakeException
import java.net.InetAddress

internal class AttemptFailure(
    val failure: PairingFailure,
    /** Stops the whole session (for example Bluetooth turned off). */
    val fatal: Boolean,
    /** Do not try this candidate again in this session. */
    val excludeCandidate: Boolean,
)

internal object FailureMapping {

    fun fromTransport(e: TransportException): AttemptFailure = when (e.kind) {
        TransportException.Kind.BLUETOOTH_DISABLED ->
            AttemptFailure(PairingFailure.BluetoothDisabled, fatal = true, excludeCandidate = true)
        TransportException.Kind.PERMISSION_DENIED ->
            AttemptFailure(PairingFailure.PermissionDenied, fatal = true, excludeCandidate = true)
        TransportException.Kind.SERVICE_NOT_FOUND, TransportException.Kind.CHARACTERISTIC_MISSING ->
            AttemptFailure(PairingFailure.NotAConnectFlowDevice, fatal = false, excludeCandidate = true)
        TransportException.Kind.DISCONNECTED ->
            AttemptFailure(PairingFailure.Disconnected, fatal = false, excludeCandidate = !e.transient)
        TransportException.Kind.SCAN_FAILED,
        TransportException.Kind.CONNECT_FAILED,
        TransportException.Kind.TIMEOUT,
        TransportException.Kind.NOTIFICATION_SETUP_FAILED ->
            AttemptFailure(PairingFailure.ConnectionFailed, fatal = false, excludeCandidate = !e.transient)
    }

    fun fromHandshake(e: HandshakeException): AttemptFailure {
        val failure = when (e.reason) {
            HandshakeException.Reason.TIMEOUT -> PairingFailure.PeerNotResponding
            HandshakeException.Reason.DISCONNECTED -> PairingFailure.Disconnected
            HandshakeException.Reason.TRANSPORT_ERROR -> PairingFailure.ConnectionFailed
            HandshakeException.Reason.PROTOCOL_VERSION_MISMATCH -> PairingFailure.ProtocolVersionMismatch
            HandshakeException.Reason.MALFORMED_MESSAGE,
            HandshakeException.Reason.UNEXPECTED_MESSAGE -> PairingFailure.ProtocolError
            HandshakeException.Reason.PEER_REJECTED -> PairingFailure.PeerRejected(e.peerErrorCode ?: -1)
            HandshakeException.Reason.LOCAL_IDENTITY_ERROR -> PairingFailure.IdentityUnavailable
            HandshakeException.Reason.ALREADY_USED -> PairingFailure.Internal
            HandshakeException.Reason.INVALID_PEER_KEY,
            HandshakeException.Reason.REFLECTED_MESSAGE,
            HandshakeException.Reason.WRONG_DEVICE_ID,
            HandshakeException.Reason.INVALID_SIGNATURE,
            HandshakeException.Reason.INVALID_MAC -> PairingFailure.AuthenticationFailed
        }
        val retryable = e.reason == HandshakeException.Reason.DISCONNECTED ||
            e.reason == HandshakeException.Reason.TRANSPORT_ERROR
        return AttemptFailure(
            failure = failure,
            fatal = failure == PairingFailure.IdentityUnavailable,
            excludeCandidate = !retryable,
        )
    }

    /**
     * When every candidate failed, report the failure the user can act on. A verification
     * failure is ranked low because it usually means "a different ConnectFlow device nearby".
     */
    fun mostRelevant(failures: List<PairingFailure>): PairingFailure? {
        val order = listOf(
            PairingFailure.ProtocolVersionMismatch::class,
            PairingFailure.PeerNotResponding::class,
            PairingFailure.PeerRejected::class,
            PairingFailure.ProtocolError::class,
            PairingFailure.Disconnected::class,
            PairingFailure.ConnectionFailed::class,
            PairingFailure.AuthenticationFailed::class,
            PairingFailure.NotAConnectFlowDevice::class,
        )
        for (kind in order) failures.firstOrNull { kind.isInstance(it) }?.let { return it }
        return failures.lastOrNull()
    }
}

internal object IpAddresses {
    /** Text form of an authenticated address hint, or null for addresses that cannot be a LAN peer. */
    fun toHint(bytes: ByteArray?): String? {
        if (bytes == null || (bytes.size != 4 && bytes.size != 16)) return null
        val address = InetAddress.getByAddress(bytes)
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isMulticastAddress) return null
        return address.hostAddress
    }
}

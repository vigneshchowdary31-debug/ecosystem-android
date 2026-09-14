package com.ecosystem.core.pairing

import com.ecosystem.core.protocol.handshake.LocalPairingIdentity
import com.ecosystem.core.protocol.link.MessageChannel
import java.util.UUID

// Ports implemented by the Android data layer. The pairing domain depends only on these.

interface PairingTransport {
    fun readiness(): TransportReadiness

    /**
     * Scans for devices advertising the pairing service. Waits up to [timeoutMillis] for the
     * first result, then [settleMillis] more to collect nearby alternatives. Returns candidates
     * strongest signal first; an empty list means none was found. Throws [TransportException].
     */
    suspend fun scanForCandidates(timeoutMillis: Long, settleMillis: Long): List<PairingCandidate>

    /**
     * Opens a connection ready for framed messages (service discovered, notifications enabled).
     * Throws [TransportException]. On failure any partially opened connection is already closed.
     */
    suspend fun connect(candidate: PairingCandidate, onPhase: (ConnectionPhase) -> Unit): PairingConnection
}

enum class TransportReadiness {
    READY,
    BLUETOOTH_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    PERMISSION_DENIED,
    LOCATION_DISABLED,
}

/**
 * A device advertising the pairing service. [handle] is an opaque transport handle (for BLE,
 * the current, possibly rotating, address). It is never an identity: trust comes only from
 * the handshake with the QR identity key.
 */
data class PairingCandidate(val handle: String, val rssi: Int)

enum class ConnectionPhase { CONNECTING, NEGOTIATING_MTU, DISCOVERING_SERVICES, ENABLING_NOTIFICATIONS, READY }

/** One open connection to one candidate. [close] is idempotent and releases every resource. */
interface PairingConnection : MessageChannel {
    fun close()
}

class TransportException(
    val kind: Kind,
    message: String,
    /** True when retrying the same candidate may succeed (for example GATT status 133). */
    val transient: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Kind {
        BLUETOOTH_DISABLED,
        PERMISSION_DENIED,
        SCAN_FAILED,
        CONNECT_FAILED,
        TIMEOUT,
        SERVICE_NOT_FOUND,
        CHARACTERISTIC_MISSING,
        NOTIFICATION_SETUP_FAILED,
        DISCONNECTED,
    }
}

interface LocalIdentitySource {
    /** Throws [IdentityUnavailableException] when the identity key cannot be used. */
    suspend fun loadIdentity(): LocalPairingIdentity
}

class IdentityUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface TrustedPeerStore {
    suspend fun savePairedPeer(peer: PairedPeer)
}

class PairedPeer(
    val deviceId: UUID,
    val name: String,
    val identityPublicKey: ByteArray,
    /** Address the Mac reported inside its authenticated message, if any. A hint only. */
    val ipAddress: String?,
    val pairedAtMillis: Long,
)

/** Supplies this device's current local-network address, if it has one, as 4 or 16 raw bytes. */
fun interface LocalAddressProvider {
    fun currentLocalAddress(): ByteArray?
}

interface PairingLogger {
    fun debug(message: String)
    fun warn(message: String, error: Throwable? = null)

    companion object {
        val NONE: PairingLogger = object : PairingLogger {
            override fun debug(message: String) {}
            override fun warn(message: String, error: Throwable?) {}
        }
    }
}

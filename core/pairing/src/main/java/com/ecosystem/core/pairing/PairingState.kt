package com.ecosystem.core.pairing

import com.ecosystem.core.protocol.qr.QrError

/**
 * Pairing state machine. Terminal states: [PairingSucceeded], [PairingFailed], [Cancelled],
 * [Disconnected] and [TimedOut].
 *
 * [Connected] is only reached after the Mac has proven, with the identity key from the QR
 * code, that it took part in this exact session. [PairingSucceeded] is only reached after the
 * Mac has also confirmed that it stored the pairing and Android has stored it too.
 */
sealed interface PairingState {
    val isActive: Boolean get() = false

    data object Idle : PairingState

    data object Scanning : PairingState {
        override val isActive: Boolean get() = true
    }

    /** A device advertising the pairing service was found; [attempt] counts connection attempts in this session. */
    data class CandidateFound(val attempt: Int) : PairingState {
        override val isActive: Boolean get() = true
    }

    data class Connecting(val attempt: Int) : PairingState {
        override val isActive: Boolean get() = true
    }

    data object DiscoveringServices : PairingState {
        override val isActive: Boolean get() = true
    }

    data object EnablingNotifications : PairingState {
        override val isActive: Boolean get() = true
    }

    data object ExchangingKeys : PairingState {
        override val isActive: Boolean get() = true
    }

    data object Authenticating : PairingState {
        override val isActive: Boolean get() = true
    }

    /** The Mac is verified; the final confirmation messages are still in flight. */
    data class Connected(val peerName: String) : PairingState {
        override val isActive: Boolean get() = true
    }

    data class PairingSucceeded(val device: PairedDeviceSummary) : PairingState

    data class PairingFailed(val failure: PairingFailure) : PairingState

    data object Cancelled : PairingState

    data object Disconnected : PairingState

    data object TimedOut : PairingState
}

data class PairedDeviceSummary(val deviceId: String, val name: String)

sealed interface PairingFailure {
    data class InvalidQr(val error: QrError) : PairingFailure
    data object BluetoothUnavailable : PairingFailure
    data object BluetoothDisabled : PairingFailure
    data object PermissionDenied : PairingFailure
    data object LocationServicesDisabled : PairingFailure
    data object IdentityUnavailable : PairingFailure

    /** No device advertising the pairing service was found. */
    data object NoDeviceFound : PairingFailure

    data object ConnectionFailed : PairingFailure

    /** A device answered, but does not host the ConnectFlow GATT service. */
    data object NotAConnectFlowDevice : PairingFailure

    /** The device accepted the connection but never answered the handshake (for example an outdated Mac app). */
    data object PeerNotResponding : PairingFailure

    data object ProtocolVersionMismatch : PairingFailure
    data object ProtocolError : PairingFailure

    /** The device failed cryptographic verification: it is not the Mac from the QR code. */
    data object AuthenticationFailed : PairingFailure

    data class PeerRejected(val code: Int) : PairingFailure

    /** The link dropped during the handshake. Reported as [PairingState.Disconnected]. */
    data object Disconnected : PairingFailure

    data object StorageFailed : PairingFailure
    data object Internal : PairingFailure
}

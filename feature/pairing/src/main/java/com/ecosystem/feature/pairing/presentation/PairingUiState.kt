package com.ecosystem.feature.pairing.presentation

import com.ecosystem.core.pairing.PairingFailure
import com.ecosystem.core.pairing.PairingState
import com.ecosystem.core.protocol.qr.QrError

enum class PairingAction { RETRY, SCAN_ANOTHER, PAIR_ANOTHER, CANCEL, ENABLE_BLUETOOTH, GRANT_PERMISSION, OPEN_LOCATION_SETTINGS }

enum class PairingOutcome { NONE, SUCCESS, FAILURE, CANCELLED }

/** Everything the pairing screen renders. Never contains key material or raw protocol payloads. */
data class PairingUiState(
    val showScanner: Boolean,
    val inProgress: Boolean,
    val title: String,
    val detail: String?,
    /** 1-based progress step while pairing is running; null otherwise. */
    val step: Int?,
    val outcome: PairingOutcome,
    val primaryAction: PairingAction?,
    val secondaryAction: PairingAction?,
) {
    companion object {
        const val TOTAL_STEPS = 4
    }
}

object PairingUiMapper {

    fun map(state: PairingState): PairingUiState = when (state) {
        PairingState.Idle -> PairingUiState(
            showScanner = true,
            inProgress = false,
            title = "Scan the pairing code on your Mac",
            detail = "Open ConnectFlow on the Mac and choose Pair a device.",
            step = null,
            outcome = PairingOutcome.NONE,
            primaryAction = null,
            secondaryAction = null,
        )
        PairingState.Scanning -> progress("Looking for your Mac…", "Keep the Mac nearby with Bluetooth on.", 1)
        is PairingState.CandidateFound -> progress("Found a nearby Mac", "Connecting…", 1)
        is PairingState.Connecting ->
            progress("Connecting…", if (state.attempt > 1) "Attempt ${state.attempt}" else null, 2)
        PairingState.DiscoveringServices,
        PairingState.EnablingNotifications -> progress("Connecting…", "Opening the pairing channel", 2)
        PairingState.ExchangingKeys -> progress("Exchanging keys…", "Creating a one-time session key", 3)
        PairingState.Authenticating ->
            progress("Verifying the Mac…", "Checking the Mac's identity against the pairing code", 3)
        is PairingState.Connected -> progress("Verified ${state.peerName}", "Finishing pairing…", 4)
        is PairingState.PairingSucceeded -> PairingUiState(
            showScanner = false,
            inProgress = false,
            title = "Paired with ${state.device.name}",
            detail = "This phone and the Mac now trust each other's identity keys.",
            step = null,
            outcome = PairingOutcome.SUCCESS,
            primaryAction = PairingAction.PAIR_ANOTHER,
            secondaryAction = null,
        )
        PairingState.Cancelled -> ended(
            "Pairing cancelled", null, PairingOutcome.CANCELLED, PairingAction.RETRY, PairingAction.SCAN_ANOTHER
        )
        PairingState.Disconnected -> failure(
            "The connection dropped",
            "Move closer to the Mac and try again.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingState.TimedOut -> failure(
            "Pairing timed out",
            "The Mac didn't finish pairing within a minute. Check that its pairing screen is still open.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        is PairingState.PairingFailed -> mapFailure(state.failure)
    }

    private fun mapFailure(failure: PairingFailure): PairingUiState = when (failure) {
        is PairingFailure.InvalidQr -> when (failure.error) {
            QrError.NOT_A_PAIRING_CODE -> failure(
                "That isn't a ConnectFlow pairing code",
                "Scan the code shown by ConnectFlow on your Mac.",
                PairingAction.SCAN_ANOTHER,
            )
            QrError.LEGACY_FORMAT -> failure(
                "Update ConnectFlow on the Mac",
                "This code comes from an older version that doesn't use the secure pairing protocol. Update the Mac app, then scan its new code.",
                PairingAction.SCAN_ANOTHER,
            )
            QrError.UNSUPPORTED_VERSION -> failure(
                "This code needs a different app version",
                "Update ConnectFlow on both devices, then scan again.",
                PairingAction.SCAN_ANOTHER,
            )
            else -> failure(
                "This pairing code can't be read",
                "The code is damaged or incomplete. Refresh it on the Mac and scan again.",
                PairingAction.SCAN_ANOTHER,
            )
        }
        PairingFailure.BluetoothDisabled -> failure(
            "Bluetooth is off",
            "Pairing uses Bluetooth to find your Mac.",
            PairingAction.ENABLE_BLUETOOTH,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.PermissionDenied -> failure(
            "Allow Nearby devices",
            "ConnectFlow needs the Nearby devices permission to find your Mac over Bluetooth.",
            PairingAction.GRANT_PERMISSION,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.LocationServicesDisabled -> failure(
            "Turn on Location",
            "On this Android version, Bluetooth scanning only works while Location is on. Turn it on, then try again.",
            PairingAction.OPEN_LOCATION_SETTINGS,
            PairingAction.RETRY,
        )
        PairingFailure.BluetoothUnavailable -> failure(
            "Bluetooth isn't available",
            "This device doesn't support Bluetooth Low Energy.",
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.IdentityUnavailable -> failure(
            "This phone's identity key can't be read",
            "Android Keystore no longer holds the key that protects it. Clear ConnectFlow's storage in Settings, then pair again.",
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.NoDeviceFound -> failure(
            "Couldn't find your Mac",
            "Make sure the Mac is nearby, Bluetooth is on and its pairing screen is open.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.ConnectionFailed -> failure(
            "Couldn't connect to the Mac",
            "Move closer to the Mac and try again.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.NotAConnectFlowDevice -> failure(
            "No ConnectFlow Mac answered",
            "The nearby device doesn't offer ConnectFlow pairing. Open the pairing screen on your Mac and try again.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.PeerNotResponding -> failure(
            "The Mac didn't respond",
            "Make sure ConnectFlow on the Mac is up to date and still showing its pairing code.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.ProtocolVersionMismatch -> failure(
            "The Mac uses a different pairing version",
            "Update ConnectFlow on both devices, then try again.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.ProtocolError -> failure(
            "Pairing was interrupted",
            "The Mac sent a message this app didn't expect. Try again.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.AuthenticationFailed -> failure(
            "Couldn't verify the Mac",
            "A nearby device answered but couldn't prove it's the Mac from this code, so nothing was saved. Scan the code on the Mac you want to pair.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        is PairingFailure.PeerRejected -> failure(
            "The Mac declined pairing",
            when (failure.code) {
                0x03 -> "The Mac couldn't verify this phone. Try again."
                0x04 -> "The Mac is busy pairing another device. Try again in a moment."
                0x07 -> "The Mac isn't showing its pairing code. Open it on the Mac and try again."
                else -> "Try again."
            },
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.Disconnected -> map(PairingState.Disconnected)
        PairingFailure.StorageFailed -> failure(
            "Couldn't save the pairing",
            "The Mac was verified, but this phone couldn't store it. Try again.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
        PairingFailure.Internal -> failure(
            "Something went wrong",
            "Try pairing again.",
            PairingAction.RETRY,
            PairingAction.SCAN_ANOTHER,
        )
    }

    private fun progress(title: String, detail: String?, step: Int) = PairingUiState(
        showScanner = false,
        inProgress = true,
        title = title,
        detail = detail,
        step = step,
        outcome = PairingOutcome.NONE,
        primaryAction = PairingAction.CANCEL,
        secondaryAction = null,
    )

    private fun failure(title: String, detail: String, primary: PairingAction, secondary: PairingAction? = null) =
        ended(title, detail, PairingOutcome.FAILURE, primary, secondary)

    private fun ended(
        title: String,
        detail: String?,
        outcome: PairingOutcome,
        primary: PairingAction,
        secondary: PairingAction?,
    ) = PairingUiState(
        showScanner = false,
        inProgress = false,
        title = title,
        detail = detail,
        step = null,
        outcome = outcome,
        primaryAction = primary,
        secondaryAction = secondary,
    )
}

package com.ecosystem.core.pairing

import com.ecosystem.core.protocol.qr.QrError
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry point for pairing with a Mac. At most one pairing session runs at a time; every
 * method is safe to call from any thread.
 */
interface PairingManager {
    val state: StateFlow<PairingState>

    /**
     * Starts pairing with the Mac whose QR code produced [qrText]. Ignored while a session is
     * already active, so repeated camera frames or a second QR code cannot start a parallel session.
     */
    fun startPairing(qrText: String): StartResult

    /** Cancels the active session. Its BLE connection is closed and its secrets are wiped. */
    fun cancelPairing()

    /**
     * Cancels any running session, waits for its cleanup, then starts a clean session with the
     * last valid QR code. Returns false when there is no QR code to retry with.
     */
    fun retryPairing(): Boolean

    /** Cancels any session and returns to [PairingState.Idle], forgetting the last QR code. */
    fun reset()
}

sealed interface StartResult {
    data object Started : StartResult
    data object IgnoredAlreadyActive : StartResult
    data class RejectedInvalidQr(val error: QrError) : StartResult
}

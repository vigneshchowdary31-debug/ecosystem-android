package com.ecosystem.core.pairing

import kotlinx.coroutines.flow.StateFlow

sealed class PairingState {
    object Idle : PairingState()
    object ParsingQr : PairingState()
    object ConnectingBle : PairingState()
    object EphemeralKeyAgreement : PairingState()
    object SignatureVerification : PairingState()
    object Success : PairingState()
    data class Failed(val reason: String) : PairingState()
}

interface PairingManager {
    val pairingState: StateFlow<PairingState>
    
    suspend fun startPairing(qrPayload: String): Boolean
    fun reset()
}

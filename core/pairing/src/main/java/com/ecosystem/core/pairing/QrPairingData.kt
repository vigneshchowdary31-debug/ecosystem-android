package com.ecosystem.core.pairing

data class QrPairingData(
    val deviceId: String,
    val name: String,
    val bleMacAddress: String,
    val publicKeyEd25519: String,
    val ephemeralPublicKeyX25519: String
)

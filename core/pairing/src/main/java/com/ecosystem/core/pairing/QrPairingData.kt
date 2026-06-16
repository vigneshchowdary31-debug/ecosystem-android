package com.ecosystem.core.pairing

data class QrPairingData(
    val deviceId: String,
    val name: String,
    val advertisingIdentifier: String,
    val publicKeyEd25519: String
)

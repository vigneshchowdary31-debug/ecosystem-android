package com.ecosystem.core.identity

/**
 * Clean domain representation of the local device identity and security credentials.
 */
data class DeviceInfo(
    val deviceId: String,
    val name: String,
    val publicKeyEd25519: String,
    val advertisingIdentifier: String,
    val createdAt: Long = 0L
)

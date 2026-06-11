package com.ecosystem.core.trusteddevices

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_devices")
data class TrustedDeviceEntity(
    @PrimaryKey 
    val deviceId: String,                    // Unique UUID of the macOS client
    val name: String,                        // Human-readable device name (e.g. "MacBook Pro")
    val bleMacAddress: String,               // Used for BLE reconnection
    val lastKnownIpAddress: String?,         // Used for future Wi-Fi socket routing
    val publicKeyEd25519: String,            // Base64-encoded macOS identity public key
    val isTrustActive: Boolean,              // Revoke trust by setting to false
    val pairingTimestamp: Long,
    val lastSeenTimestamp: Long
)

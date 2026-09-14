package com.ecosystem.core.trusteddevices

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a trusted Mac (schema v3). The logical identity is [deviceId]; the BLE address
 * column from v1/v2 was dropped because Mac BLE addresses rotate and must never identify a device.
 */
@Entity(tableName = "trusted_devices")
data class TrustedDeviceEntity(
    /** Canonical lower-case UUID of the Mac (from its pairing QR code). */
    @PrimaryKey
    val deviceId: String,
    val name: String,
    /** Address the Mac reported inside its signed MAC_AUTH message. A routing hint for Sprint 2, never trusted alone. */
    val lastKnownIpAddress: String?,
    /** Base64 (RFC 4648, padded) of the Mac's 32-byte Ed25519 identity key. */
    val publicKeyEd25519: String,
    /** False once trust is revoked; revoked rows are kept for audit but not returned as trusted. */
    val isTrustActive: Boolean,
    /** When pairing with this identity key first succeeded. Kept across re-pairing with the same key. */
    val pairingTimestamp: Long,
    val lastSeenTimestamp: Long,
)

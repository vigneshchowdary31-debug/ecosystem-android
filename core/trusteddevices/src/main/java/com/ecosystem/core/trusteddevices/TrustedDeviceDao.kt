package com.ecosystem.core.trusteddevices

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TrustedDeviceDao {

    @Query("SELECT * FROM trusted_devices WHERE isTrustActive = 1 ORDER BY pairingTimestamp DESC")
    abstract fun observeActive(): Flow<List<TrustedDeviceEntity>>

    @Query("SELECT * FROM trusted_devices WHERE isTrustActive = 1 ORDER BY pairingTimestamp DESC")
    abstract suspend fun getActive(): List<TrustedDeviceEntity>

    @Query("SELECT * FROM trusted_devices WHERE deviceId = :deviceId LIMIT 1")
    abstract suspend fun getById(deviceId: String): TrustedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrReplace(device: TrustedDeviceEntity)

    /** Updates liveness; keeps the previous address when [ipAddress] is null. Returns the number of rows updated. */
    @Query(
        "UPDATE trusted_devices SET lastSeenTimestamp = :timestamp, " +
            "lastKnownIpAddress = COALESCE(:ipAddress, lastKnownIpAddress) WHERE deviceId = :deviceId"
    )
    abstract suspend fun recordSeen(deviceId: String, timestamp: Long, ipAddress: String?): Int

    @Query("UPDATE trusted_devices SET isTrustActive = 0 WHERE deviceId = :deviceId")
    abstract suspend fun revoke(deviceId: String): Int

    @Query("DELETE FROM trusted_devices WHERE deviceId = :deviceId")
    abstract suspend fun delete(deviceId: String): Int

    /**
     * Deterministic upsert after a successful pairing:
     * - same device and same identity key: refresh name, address, last-seen and re-activate
     *   trust, keeping the original pairing time;
     * - new device, or the same device ID with a new identity key: store a new trust
     *   relationship starting now.
     */
    @Transaction
    open suspend fun upsertPaired(device: TrustedDeviceEntity) {
        val existing = getById(device.deviceId)
        val merged = if (existing != null && existing.publicKeyEd25519 == device.publicKeyEd25519) {
            device.copy(
                pairingTimestamp = existing.pairingTimestamp,
                lastKnownIpAddress = device.lastKnownIpAddress ?: existing.lastKnownIpAddress,
            )
        } else {
            device
        }
        insertOrReplace(merged)
    }
}

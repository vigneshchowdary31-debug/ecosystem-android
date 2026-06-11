package com.ecosystem.core.trusteddevices

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedDeviceDao {
    @Query("SELECT * FROM trusted_devices WHERE isTrustActive = 1")
    fun getActiveTrustedDevicesFlow(): Flow<List<TrustedDeviceEntity>>

    @Query("SELECT * FROM trusted_devices WHERE isTrustActive = 1")
    suspend fun getActiveTrustedDevices(): List<TrustedDeviceEntity>

    @Query("SELECT * FROM trusted_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): TrustedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: TrustedDeviceEntity)

    @Query("UPDATE trusted_devices SET lastKnownIpAddress = :ipAddress WHERE deviceId = :deviceId")
    suspend fun updateDeviceIpAddress(deviceId: String, ipAddress: String?)

    @Query("UPDATE trusted_devices SET lastSeenTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastSeen(deviceId: String, timestamp: Long)

    @Delete
    suspend fun deleteDevice(device: TrustedDeviceEntity)
}

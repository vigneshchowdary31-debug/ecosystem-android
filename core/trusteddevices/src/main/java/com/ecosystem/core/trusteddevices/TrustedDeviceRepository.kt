package com.ecosystem.core.trusteddevices

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TrustedDeviceRepository {
    fun getActiveTrustedDevicesFlow(): Flow<List<TrustedDeviceEntity>>
    suspend fun getActiveTrustedDevices(): List<TrustedDeviceEntity>
    suspend fun getDeviceById(deviceId: String): TrustedDeviceEntity?
    suspend fun addTrustedDevice(device: TrustedDeviceEntity)
    suspend fun removeTrustedDevice(device: TrustedDeviceEntity)
    suspend fun updateDeviceIpAddress(deviceId: String, ipAddress: String?)
    suspend fun updateLastSeen(deviceId: String, timestamp: Long)
}

@Singleton
class TrustedDeviceRepositoryImpl @Inject constructor(
    private val trustedDeviceDao: TrustedDeviceDao
) : TrustedDeviceRepository {

    override fun getActiveTrustedDevicesFlow(): Flow<List<TrustedDeviceEntity>> {
        return trustedDeviceDao.getActiveTrustedDevicesFlow()
    }

    override suspend fun getActiveTrustedDevices(): List<TrustedDeviceEntity> {
        return trustedDeviceDao.getActiveTrustedDevices()
    }

    override suspend fun getDeviceById(deviceId: String): TrustedDeviceEntity? {
        return trustedDeviceDao.getDeviceById(deviceId)
    }

    override suspend fun addTrustedDevice(device: TrustedDeviceEntity) {
        trustedDeviceDao.insertOrUpdateDevice(device)
    }

    override suspend fun removeTrustedDevice(device: TrustedDeviceEntity) {
        trustedDeviceDao.deleteDevice(device)
    }

    override suspend fun updateDeviceIpAddress(deviceId: String, ipAddress: String?) {
        trustedDeviceDao.updateDeviceIpAddress(deviceId, ipAddress)
    }

    override suspend fun updateLastSeen(deviceId: String, timestamp: Long) {
        trustedDeviceDao.updateLastSeen(deviceId, timestamp)
    }
}

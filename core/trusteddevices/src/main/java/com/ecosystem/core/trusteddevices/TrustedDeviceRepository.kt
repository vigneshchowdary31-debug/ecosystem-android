package com.ecosystem.core.trusteddevices

import com.ecosystem.core.common.log.AppLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/** A Mac this phone trusts. Only records that pass validation are ever returned. */
data class TrustedDevice(
    val deviceId: String,
    val name: String,
    /** Base64 of the 32-byte Ed25519 identity key. */
    val publicKeyEd25519: String,
    val lastKnownIpAddress: String?,
    val isTrustActive: Boolean,
    val pairedAt: Long,
    val lastSeenAt: Long,
)

interface TrustedDeviceRepository {
    fun observeTrustedDevices(): Flow<List<TrustedDevice>>
    suspend fun getTrustedDevices(): List<TrustedDevice>
    suspend fun getDevice(deviceId: String): TrustedDevice?

    /** Stores the result of a verified pairing. See [TrustedDeviceDao.upsertPaired] for the upsert rules. */
    suspend fun savePairedDevice(deviceId: String, name: String, publicKey: ByteArray, ipAddress: String?, timestamp: Long)

    /** Records that the device was reached at [timestamp] (and, if known, at [ipAddress]). */
    suspend fun recordSeen(deviceId: String, timestamp: Long, ipAddress: String?)

    /** Stops trusting the device without deleting the record. */
    suspend fun revoke(deviceId: String)

    suspend fun delete(deviceId: String)
}

@Singleton
class TrustedDeviceRepositoryImpl @Inject constructor(
    private val dao: TrustedDeviceDao,
) : TrustedDeviceRepository {

    override fun observeTrustedDevices(): Flow<List<TrustedDevice>> =
        dao.observeActive().map { rows -> rows.mapNotNull(TrustedDeviceRecords::toDomain) }

    override suspend fun getTrustedDevices(): List<TrustedDevice> =
        dao.getActive().mapNotNull(TrustedDeviceRecords::toDomain)

    override suspend fun getDevice(deviceId: String): TrustedDevice? =
        TrustedDeviceRecords.canonicalId(deviceId)?.let { dao.getById(it) }?.let(TrustedDeviceRecords::toDomain)

    override suspend fun savePairedDevice(
        deviceId: String,
        name: String,
        publicKey: ByteArray,
        ipAddress: String?,
        timestamp: Long,
    ) {
        val canonicalId = requireNotNull(TrustedDeviceRecords.canonicalId(deviceId)) { "Device ID must be a UUID" }
        require(publicKey.size == TrustedDeviceRecords.PUBLIC_KEY_SIZE) { "Identity key must be 32 bytes" }
        require(TrustedDeviceRecords.isValidName(name)) { "Invalid device name" }
        dao.upsertPaired(
            TrustedDeviceEntity(
                deviceId = canonicalId,
                name = name,
                lastKnownIpAddress = ipAddress,
                publicKeyEd25519 = Base64.getEncoder().encodeToString(publicKey),
                isTrustActive = true,
                pairingTimestamp = timestamp,
                lastSeenTimestamp = timestamp,
            )
        )
    }

    override suspend fun recordSeen(deviceId: String, timestamp: Long, ipAddress: String?) {
        TrustedDeviceRecords.canonicalId(deviceId)?.let { dao.recordSeen(it, timestamp, ipAddress) }
    }

    override suspend fun revoke(deviceId: String) {
        TrustedDeviceRecords.canonicalId(deviceId)?.let { dao.revoke(it) }
    }

    override suspend fun delete(deviceId: String) {
        TrustedDeviceRecords.canonicalId(deviceId)?.let { dao.delete(it) }
    }
}

/** Validation between Room rows and the domain model. Malformed rows are skipped, never trusted. */
internal object TrustedDeviceRecords {
    const val PUBLIC_KEY_SIZE = 32
    private const val MAX_NAME_BYTES = 128
    private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun canonicalId(deviceId: String): String? = deviceId.takeIf(UUID_PATTERN::matches)?.lowercase()

    fun isValidName(name: String): Boolean =
        name.isNotBlank() && name.toByteArray(Charsets.UTF_8).size <= MAX_NAME_BYTES && name.none(Character::isISOControl)

    fun toDomain(row: TrustedDeviceEntity): TrustedDevice? {
        val validKey = try {
            Base64.getDecoder().decode(row.publicKeyEd25519).size == PUBLIC_KEY_SIZE
        } catch (e: IllegalArgumentException) {
            false
        }
        if (canonicalId(row.deviceId) != row.deviceId || !validKey || !isValidName(row.name) || row.pairingTimestamp <= 0) {
            AppLog.w("TrustedDeviceRepository", "Skipping malformed trusted-device record")
            return null
        }
        return TrustedDevice(
            deviceId = row.deviceId,
            name = row.name,
            publicKeyEd25519 = row.publicKeyEd25519,
            lastKnownIpAddress = row.lastKnownIpAddress,
            isTrustActive = row.isTrustActive,
            pairedAt = row.pairingTimestamp,
            lastSeenAt = row.lastSeenTimestamp,
        )
    }
}

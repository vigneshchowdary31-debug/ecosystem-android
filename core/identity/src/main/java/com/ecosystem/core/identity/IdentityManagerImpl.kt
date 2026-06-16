package com.ecosystem.core.identity

import android.os.Build
import com.ecosystem.core.security.CryptoManager
import com.ecosystem.core.security.SecureStorage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentityManagerImpl @Inject constructor(
    private val localIdentityDao: LocalIdentityDao,
    private val cryptoManager: CryptoManager,
    private val secureStorage: SecureStorage
) : IdentityManager {

    companion object {
        private const val IDENTITY_ALIAS = "primary_device_identity"
        private const val KEY_SECURE_DEVICE_ID = "identity_device_id"
        private const val KEY_SECURE_ADVERTISING_ID = "identity_advertising_id"
    }

    override suspend fun getOrCreateIdentity(): DeviceInfo {
        // Try retrieving from Room database first
        val dbIdentity = localIdentityDao.getIdentity(IDENTITY_ALIAS)
        // Also verify secure storage has the values
        val secureDeviceId = secureStorage.getString(KEY_SECURE_DEVICE_ID)
        val securePublicKey = cryptoManager.getIdentityPublicKey(IDENTITY_ALIAS)
        val secureAdvertisingId = secureStorage.getString(KEY_SECURE_ADVERTISING_ID)

        if (dbIdentity != null && secureDeviceId != null && securePublicKey != null && secureAdvertisingId != null) {
            return DeviceInfo(
                deviceId = secureDeviceId,
                name = dbIdentity.name,
                publicKeyEd25519 = securePublicKey,
                advertisingIdentifier = secureAdvertisingId,
                createdAt = dbIdentity.createdAt
            )
        }

        // If not found, generate secure identity
        val deviceId = secureDeviceId ?: UUID.randomUUID().toString()
        val deviceName = getDeviceName()
        val advertisingId = secureAdvertisingId ?: UUID.randomUUID().toString()
        
        // Generate new long-term Ed25519 keypair in Android Keystore / Tink
        val publicKeyBase64 = securePublicKey ?: cryptoManager.generateIdentityKeyPair(IDENTITY_ALIAS)

        // Securely persist the generated UUIDs via keystore-backed SecureStorage
        secureStorage.putString(KEY_SECURE_DEVICE_ID, deviceId)
        secureStorage.putString(KEY_SECURE_ADVERTISING_ID, advertisingId)

        val newIdentity = LocalIdentityEntity(
            idAlias = IDENTITY_ALIAS,
            deviceId = deviceId,
            name = deviceName,
            publicKeyEd25519 = publicKeyBase64,
            advertisingIdentifier = advertisingId,
            createdAt = System.currentTimeMillis()
        )

        localIdentityDao.insertIdentity(newIdentity)

        return DeviceInfo(
            deviceId = deviceId,
            name = deviceName,
            publicKeyEd25519 = publicKeyBase64,
            advertisingIdentifier = advertisingId,
            createdAt = newIdentity.createdAt
        )
    }

    override suspend fun getIdentity(): DeviceInfo? {
        val dbIdentity = localIdentityDao.getIdentity(IDENTITY_ALIAS) ?: return null
        val secureDeviceId = secureStorage.getString(KEY_SECURE_DEVICE_ID) ?: return null
        val securePublicKey = cryptoManager.getIdentityPublicKey(IDENTITY_ALIAS) ?: return null
        val secureAdvertisingId = secureStorage.getString(KEY_SECURE_ADVERTISING_ID) ?: return null

        return DeviceInfo(
            deviceId = secureDeviceId,
            name = dbIdentity.name,
            publicKeyEd25519 = securePublicKey,
            advertisingIdentifier = secureAdvertisingId,
            createdAt = dbIdentity.createdAt
        )
    }

    override suspend fun signChallenge(challenge: ByteArray): ByteArray {
        return cryptoManager.signWithIdentityKey(IDENTITY_ALIAS, challenge)
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        val model = Build.MODEL ?: "Android Device"
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            val manufacturerTitle = manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            "$manufacturerTitle $model"
        }
    }
}

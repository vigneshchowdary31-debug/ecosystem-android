package com.ecosystem.core.identity

import android.os.Build
import com.ecosystem.core.common.DispatcherProvider
import com.ecosystem.core.common.log.AppLog
import com.ecosystem.core.security.CryptoManager
import com.ecosystem.core.security.IdentityKeyState
import com.ecosystem.core.security.IdentityKeyUnavailableException
import com.ecosystem.core.security.SecureRead
import com.ecosystem.core.security.SecureStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device identity: a stable UUID device ID, a stable advertising UUID and an Ed25519 key pair.
 * The IDs and the private key live in [SecureStorage]; the Room row only caches the display
 * name and creation time, so losing the database never changes the identity.
 */
@Singleton
class IdentityManagerImpl @Inject constructor(
    private val localIdentityDao: LocalIdentityDao,
    private val cryptoManager: CryptoManager,
    private val secureStorage: SecureStorage,
    private val dispatchers: DispatcherProvider,
) : IdentityManager {

    private val mutex = Mutex()

    override suspend fun getOrCreateIdentity(): DeviceInfo =
        mutex.withLock { withContext(dispatchers.io) { loadOrCreate() } }

    override suspend fun getIdentity(): DeviceInfo? = mutex.withLock {
        withContext(dispatchers.io) {
            val deviceId = readUuid(KEY_DEVICE_ID) ?: return@withContext null
            val advertisingId = readUuid(KEY_ADVERTISING_ID) ?: return@withContext null
            val publicKey = when (val state = cryptoManager.identityKeyState(IDENTITY_ALIAS)) {
                is IdentityKeyState.Present -> state.publicKey
                IdentityKeyState.Missing -> return@withContext null
                is IdentityKeyState.Unreadable -> throw unavailable("identity key", state.cause)
            }
            val row = localIdentityDao.getIdentity(IDENTITY_ALIAS)
            DeviceInfo(
                deviceId = deviceId,
                name = row?.name ?: deviceName(),
                publicKeyEd25519 = Base64.getEncoder().encodeToString(publicKey),
                advertisingIdentifier = advertisingId,
                createdAt = row?.createdAt ?: 0L,
            )
        }
    }

    override suspend fun sign(data: ByteArray): ByteArray =
        withContext(dispatchers.io) { cryptoManager.sign(IDENTITY_ALIAS, data) }

    override suspend fun resetIdentity(): DeviceInfo = mutex.withLock {
        withContext(dispatchers.io) {
            AppLog.w(TAG, "Resetting device identity at the user's request")
            secureStorage.remove(KEY_DEVICE_ID)
            secureStorage.remove(KEY_ADVERTISING_ID)
            cryptoManager.deleteIdentityKey(IDENTITY_ALIAS)
            localIdentityDao.deleteIdentity(IDENTITY_ALIAS)
            loadOrCreate()
        }
    }

    /** Must be called with [mutex] held. */
    private suspend fun loadOrCreate(): DeviceInfo {
        val storedDeviceId = readUuid(KEY_DEVICE_ID)
        val storedAdvertisingId = readUuid(KEY_ADVERTISING_ID)
        val publicKey = when (val state = cryptoManager.identityKeyState(IDENTITY_ALIAS)) {
            is IdentityKeyState.Present -> state.publicKey
            is IdentityKeyState.Unreadable -> throw unavailable("identity key", state.cause)
            IdentityKeyState.Missing -> {
                if (storedDeviceId != null) AppLog.w(TAG, "Identity key missing for an existing device ID; generating a new key")
                cryptoManager.generateIdentityKeyPair(IDENTITY_ALIAS)
            }
        }
        val deviceId = storedDeviceId ?: UUID.randomUUID().toString().also { secureStorage.putString(KEY_DEVICE_ID, it) }
        val advertisingId = storedAdvertisingId
            ?: UUID.randomUUID().toString().also { secureStorage.putString(KEY_ADVERTISING_ID, it) }
        val publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey)

        val row = localIdentityDao.getIdentity(IDENTITY_ALIAS)
        val identity = LocalIdentityEntity(
            idAlias = IDENTITY_ALIAS,
            deviceId = deviceId,
            name = row?.name ?: deviceName(),
            publicKeyEd25519 = publicKeyBase64,
            advertisingIdentifier = advertisingId,
            createdAt = row?.createdAt ?: System.currentTimeMillis(),
        )
        if (row != identity) localIdentityDao.insertIdentity(identity)

        return DeviceInfo(
            deviceId = deviceId,
            name = identity.name,
            publicKeyEd25519 = publicKeyBase64,
            advertisingIdentifier = advertisingId,
            createdAt = identity.createdAt,
        )
    }

    /**
     * Returns the stored UUID, null when absent, and throws when a value exists but cannot be
     * used: a silent replacement would change this device's identity for every paired Mac.
     */
    private fun readUuid(key: String): String? = when (val read = secureStorage.readBytes(key)) {
        SecureRead.Missing -> null
        is SecureRead.Unreadable -> throw unavailable(key, read.cause)
        is SecureRead.Present -> {
            val text = String(read.value, Charsets.UTF_8)
            read.wipe()
            if (!UUID_PATTERN.matches(text)) throw unavailable(key, null)
            if (read.isLegacyFormat) secureStorage.putString(key, text)
            text.lowercase()
        }
    }

    private fun unavailable(what: String, cause: Throwable?) =
        IdentityKeyUnavailableException("Stored $what cannot be read; identity reset required", cause)

    private fun deviceName(): String {
        val manufacturer = Build.MANUFACTURER ?: "Android"
        val model = Build.MODEL ?: "device"
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.replaceFirstChar { it.titlecase() }
        } else {
            "${manufacturer.replaceFirstChar { it.titlecase() }} $model"
        }
    }

    private companion object {
        const val TAG = "IdentityManager"
        const val IDENTITY_ALIAS = "primary_device_identity"
        const val KEY_DEVICE_ID = "identity_device_id"
        const val KEY_ADVERTISING_ID = "identity_advertising_id"
        val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}

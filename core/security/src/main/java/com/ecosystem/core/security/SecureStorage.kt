package com.ecosystem.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

interface SecureStorage {
    fun putString(key: String, value: String)

    /** Null when the value is missing or cannot be decrypted. Use [readBytes] where that difference matters. */
    fun getString(key: String): String?

    /** Encrypts and synchronously persists [value]. Throws if it cannot be written. */
    fun putBytes(key: String, value: ByteArray)

    fun readBytes(key: String): SecureRead

    fun remove(key: String)

    fun clear()
}

sealed interface SecureRead {
    data object Missing : SecureRead

    /** [isLegacyFormat] marks values written before the v2 format (no AAD, Base64 text payload). */
    class Present(val value: ByteArray, val isLegacyFormat: Boolean) : SecureRead {
        fun wipe() = value.fill(0)
    }

    /** A value exists but cannot be decrypted, typically because the Keystore key was lost. */
    class Unreadable(val cause: Throwable) : SecureRead
}

/**
 * SharedPreferences whose values are sealed with the Keystore AES-GCM key.
 *
 * Stored format (v2): `"v2:" + Base64(iv || ciphertext || tag)`, sealed with associated data
 * `"ecosystem_secure_prefs/<key>"` so a ciphertext cannot be moved to another key. Values
 * from earlier builds have no prefix and no associated data; they stay readable and are
 * reported as legacy so callers can re-store them.
 *
 * Writes use `commit()` so identity material is on disk before it is used.
 */
@Singleton
class SecureStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager,
) : SecureStorage {

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun putBytes(key: String, value: ByteArray) {
        val sealed = keystoreManager.encrypt(value, associatedData(key))
        val stored = FORMAT_PREFIX + Base64.getEncoder().encodeToString(sealed)
        check(preferences.edit().putString(key, stored).commit()) { "Could not persist secure value '$key'" }
    }

    override fun readBytes(key: String): SecureRead {
        val stored = preferences.getString(key, null) ?: return SecureRead.Missing
        return try {
            if (stored.startsWith(FORMAT_PREFIX)) {
                val sealed = Base64.getDecoder().decode(stored.substring(FORMAT_PREFIX.length))
                SecureRead.Present(keystoreManager.decrypt(sealed, associatedData(key)), isLegacyFormat = false)
            } else {
                // Legacy values were Base64.DEFAULT (line-wrapped) and sealed without associated data.
                SecureRead.Present(keystoreManager.decrypt(Base64.getMimeDecoder().decode(stored), null), isLegacyFormat = true)
            }
        } catch (e: Exception) {
            SecureRead.Unreadable(e)
        }
    }

    override fun putString(key: String, value: String) = putBytes(key, value.toByteArray(Charsets.UTF_8))

    override fun getString(key: String): String? =
        (readBytes(key) as? SecureRead.Present)?.let { String(it.value, Charsets.UTF_8) }

    // commit(), not apply(): deleting identity material must be on disk before the caller
    // creates replacement values, otherwise a crash could resurrect the old ones.
    @SuppressLint("ApplySharedPref")
    override fun remove(key: String) {
        preferences.edit().remove(key).commit()
    }

    @SuppressLint("ApplySharedPref")
    override fun clear() {
        preferences.edit().clear().commit()
    }

    private fun associatedData(key: String): ByteArray = "$PREFS_NAME/$key".toByteArray(Charsets.UTF_8)

    companion object {
        const val PREFS_NAME = "ecosystem_secure_prefs"
        const val FORMAT_PREFIX = "v2:"
    }
}

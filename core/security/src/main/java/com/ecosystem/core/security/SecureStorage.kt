package com.ecosystem.core.security

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

interface SecureStorage {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
    fun clear()
}

@Singleton
class SecureStorageImpl @Inject constructor(
    private val context: Context,
    private val keystoreManager: KeystoreManager
) : SecureStorage {

    companion object {
        private const val PREFS_NAME = "ecosystem_secure_prefs"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun putString(key: String, value: String) {
        val encryptedValue = keystoreManager.encrypt(value)
        sharedPreferences.edit().putString(key, encryptedValue).apply()
    }

    override fun getString(key: String): String? {
        val encryptedValue = sharedPreferences.getString(key, null) ?: return null
        return try {
            keystoreManager.decrypt(encryptedValue)
        } catch (e: Exception) {
            null
        }
    }

    override fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}

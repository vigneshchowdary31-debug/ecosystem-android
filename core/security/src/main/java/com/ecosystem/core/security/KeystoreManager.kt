package com.ecosystem.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM wrapping key held in Android Keystore. The key cannot be exported and is
 * generated in StrongBox when the device has one (TEE otherwise). It protects values at rest
 * in [SecureStorage].
 *
 * It does not make the Ed25519 identity key hardware-backed: that key is generated in
 * software (Android Keystore offers no Ed25519 at this app's minSdk), stored only in this
 * encrypted form and decrypted briefly in app memory to sign.
 */
@Singleton
class KeystoreManager @Inject constructor() {

    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }

    /** Returns the 12-byte IV followed by the ciphertext and 16-byte GCM tag. */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        associatedData?.let(cipher::updateAAD)
        val ciphertext = cipher.doFinal(plaintext)
        return cipher.iv + ciphertext
    }

    /** Throws [KeystoreUnavailableException] when the key is gone or the value does not authenticate. */
    fun decrypt(sealed: ByteArray, associatedData: ByteArray?): ByteArray {
        if (sealed.size < GCM_IV_LENGTH + GCM_TAG_LENGTH_BYTES) {
            throw KeystoreUnavailableException("Sealed value is too short")
        }
        val key = existingKey() ?: throw KeystoreUnavailableException("The Keystore wrapping key is missing")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, sealed, 0, GCM_IV_LENGTH))
            associatedData?.let(cipher::updateAAD)
            cipher.doFinal(sealed, GCM_IV_LENGTH, sealed.size - GCM_IV_LENGTH)
        } catch (e: GeneralSecurityException) {
            throw KeystoreUnavailableException("The value could not be decrypted", e)
        }
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey = existingKey() ?: generateKey()

    private fun existingKey(): SecretKey? = try {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    } catch (e: UnrecoverableKeyException) {
        null
    }

    private fun generateKey(): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generateKey(strongBox = true)
            } catch (e: ProviderException) {
                // StrongBoxUnavailableException: fall back to the TEE-backed Keystore.
            }
        }
        return generateKey(strongBox = false)
    }

    private fun generateKey(strongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) builder.setIsStrongBoxBacked(true)
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(builder.build())
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "CompanionAppSecureKey"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_TAG_LENGTH_BYTES = 16
    }
}

class KeystoreUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

package com.ecosystem.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Base64

/** Runs against the real Android Keystore on a device or emulator. */
@RunWith(AndroidJUnit4::class)
class KeystoreStorageInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val keystoreManager = KeystoreManager()
    private lateinit var storage: SecureStorageImpl

    @Before
    fun setUp() {
        context.getSharedPreferences(SecureStorageImpl.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        storage = SecureStorageImpl(context, keystoreManager)
    }

    @Test
    fun valuesRoundTripThroughTheKeystore() {
        storage.putBytes("round_trip", byteArrayOf(1, 2, 3))
        val read = storage.readBytes("round_trip") as SecureRead.Present
        assertArrayEquals(byteArrayOf(1, 2, 3), read.value)
        assertFalse(read.isLegacyFormat)
    }

    @Test
    fun ciphertextMovedToAnotherKeyDoesNotDecrypt() {
        storage.putString("device_id", "abc")
        val prefs = context.getSharedPreferences(SecureStorageImpl.PREFS_NAME, Context.MODE_PRIVATE)
        val sealed = prefs.getString("device_id", null)
        prefs.edit().putString("other_key", sealed).commit()
        assertTrue(storage.readBytes("other_key") is SecureRead.Unreadable)
    }

    @Test
    fun valuesWrittenByTheLegacyFormatStayReadable() {
        val legacySealed = keystoreManager.encrypt("legacy".toByteArray(), associatedData = null)
        val prefs = context.getSharedPreferences(SecureStorageImpl.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("legacy_key", android.util.Base64.encodeToString(legacySealed, android.util.Base64.DEFAULT)).commit()

        val read = storage.readBytes("legacy_key") as SecureRead.Present
        assertTrue(read.isLegacyFormat)
        assertEquals("legacy", String(read.value))
    }

    @Test
    fun legacyIdentityKeyIsMigratedAndStillSigns() {
        val keyPair = Ed25519Sign.KeyPair.newKeyPair()
        val legacySealed = keystoreManager.encrypt(
            Base64.getEncoder().encodeToString(keyPair.privateKey).toByteArray(), associatedData = null
        )
        context.getSharedPreferences(SecureStorageImpl.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("identity_prv", android.util.Base64.encodeToString(legacySealed, android.util.Base64.DEFAULT))
            .commit()

        val cryptoManager = CryptoManagerImpl(storage)
        val signature = cryptoManager.sign("identity", byteArrayOf(4, 2))
        Ed25519Verify(keyPair.publicKey).verify(signature, byteArrayOf(4, 2))
        assertFalse((storage.readBytes("identity_prv") as SecureRead.Present).isLegacyFormat)
    }

    @Test
    fun generatedIdentityKeySurvivesANewInstance() {
        val publicKey = CryptoManagerImpl(storage).generateIdentityKeyPair("persisted")
        val reloaded = CryptoManagerImpl(SecureStorageImpl(context, KeystoreManager()))
        assertArrayEquals(publicKey, (reloaded.identityKeyState("persisted") as IdentityKeyState.Present).publicKey)
    }
}

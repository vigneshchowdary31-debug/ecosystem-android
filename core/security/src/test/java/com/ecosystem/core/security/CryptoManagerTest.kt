package com.ecosystem.core.security

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Base64

class CryptoManagerTest {

    private val storage = FakeSecureStorage()
    private val cryptoManager = CryptoManagerImpl(storage)
    private val alias = "test_alias"

    @Test
    fun `generated key is stored as a raw 32-byte seed, never as text`() {
        val publicKey = cryptoManager.generateIdentityKeyPair(alias)
        assertEquals(32, publicKey.size)
        val stored = storage.values["${alias}_prv"] as SecureRead.Present
        assertEquals(32, stored.value.size)
        assertFalse(stored.isLegacyFormat)
        val state = cryptoManager.identityKeyState(alias) as IdentityKeyState.Present
        assertArrayEquals(publicKey, state.publicKey)
    }

    @Test
    fun `signatures verify against the returned public key`() {
        val publicKey = cryptoManager.generateIdentityKeyPair(alias)
        val data = "transcript".toByteArray()
        val signature = cryptoManager.sign(alias, data)
        Ed25519Verify(publicKey).verify(signature, data)
    }

    @Test
    fun `legacy base64 text key is still usable and is migrated to raw bytes`() {
        val keyPair = Ed25519Sign.KeyPair.newKeyPair()
        val legacyText = Base64.getEncoder().encodeToString(keyPair.privateKey)
        storage.values["${alias}_prv"] = SecureRead.Present(legacyText.toByteArray(), isLegacyFormat = true)

        val signature = cryptoManager.sign(alias, byteArrayOf(1, 2, 3))
        Ed25519Verify(keyPair.publicKey).verify(signature, byteArrayOf(1, 2, 3))

        val migrated = storage.values["${alias}_prv"] as SecureRead.Present
        assertFalse(migrated.isLegacyFormat)
        assertArrayEquals(keyPair.privateKey, migrated.value)
    }

    @Test
    fun `missing key is reported and signing fails explicitly`() {
        assertEquals(IdentityKeyState.Missing, cryptoManager.identityKeyState(alias))
        expectUnavailable { cryptoManager.sign(alias, byteArrayOf(1)) }
    }

    @Test
    fun `undecryptable key is reported as unreadable and never regenerated`() {
        storage.values["${alias}_prv"] = SecureRead.Unreadable(KeystoreUnavailableException("key lost"))
        assertTrue(cryptoManager.identityKeyState(alias) is IdentityKeyState.Unreadable)
        expectUnavailable { cryptoManager.sign(alias, byteArrayOf(1)) }
        assertTrue(storage.values["${alias}_prv"] is SecureRead.Unreadable)
    }

    @Test
    fun `corrupted key length is unreadable`() {
        storage.values["${alias}_prv"] = SecureRead.Present(ByteArray(31), isLegacyFormat = false)
        assertTrue(cryptoManager.identityKeyState(alias) is IdentityKeyState.Unreadable)
    }

    @Test
    fun `delete removes the key`() {
        cryptoManager.generateIdentityKeyPair(alias)
        cryptoManager.deleteIdentityKey(alias)
        assertEquals(IdentityKeyState.Missing, cryptoManager.identityKeyState(alias))
        assertTrue(storage.values.isEmpty())
    }

    private fun expectUnavailable(block: () -> Unit) {
        try {
            block()
            fail("Expected IdentityKeyUnavailableException")
        } catch (expected: IdentityKeyUnavailableException) {
        }
    }
}

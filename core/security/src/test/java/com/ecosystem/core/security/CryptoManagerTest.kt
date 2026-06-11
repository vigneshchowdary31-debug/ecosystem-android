package com.ecosystem.core.security

import android.util.Base64
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    private val secureStorage: SecureStorage = mockk()
    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
        every { Base64.decode(any<ByteArray>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<ByteArray>())
        }
        cryptoManager = CryptoManagerImpl(secureStorage)
    }

    @Test
    fun `generateIdentityKeyPair generates and saves public and private keys`() {
        // Given
        val alias = "test_alias"
        val prvSlot = slot<String>()
        val pubSlot = slot<String>()

        every { secureStorage.putString(eq("${alias}_prv"), capture(prvSlot)) } returns Unit
        every { secureStorage.putString(eq("${alias}_pub"), capture(pubSlot)) } returns Unit

        // When
        val pubKeyBase64 = cryptoManager.generateIdentityKeyPair(alias)

        // Then
        assertNotNull(pubKeyBase64)
        assertTrue(prvSlot.isCaptured)
        assertTrue(pubSlot.isCaptured)
        assertEquals(pubKeyBase64, pubSlot.captured)

        verify { secureStorage.putString("${alias}_prv", any()) }
        verify { secureStorage.putString("${alias}_pub", any()) }
    }

    @Test
    fun `signAndVerify succeeds with generated keys`() {
        // Given
        val alias = "test_alias"
        val data = "Hello Continuity World!".toByteArray()

        // Generate actual Tink keys for mocking the storage
        val keyPair = com.google.crypto.tink.subtle.Ed25519Sign.KeyPair.newKeyPair()
        val privateKeyBase64 = java.util.Base64.getEncoder().encodeToString(keyPair.privateKey)
        val publicKeyBase64 = java.util.Base64.getEncoder().encodeToString(keyPair.publicKey)

        every { secureStorage.getString("${alias}_prv") } returns privateKeyBase64

        // When
        val signature = cryptoManager.signWithIdentityKey(alias, data)
        val isVerified = cryptoManager.verifySignature(publicKeyBase64, data, signature)

        // Then
        assertNotNull(signature)
        assertTrue(isVerified)
    }

    @Test
    fun `computeSharedSessionKey generates identical keys for both peers`() {
        // Given
        val (alicePrivateKey, alicePublicKeyBase64) = cryptoManager.generateEphemeralKeyPair()
        val (bobPrivateKey, bobPublicKeyBase64) = cryptoManager.generateEphemeralKeyPair()

        // When
        val aliceSessionKey = cryptoManager.computeSharedSessionKey(alicePrivateKey, bobPublicKeyBase64)
        val bobSessionKey = cryptoManager.computeSharedSessionKey(bobPrivateKey, alicePublicKeyBase64)

        // Then
        assertNotNull(aliceSessionKey)
        assertNotNull(bobSessionKey)
        assertEquals(32, aliceSessionKey.size)
        assertEquals(32, bobSessionKey.size)
        
        // Assert that Bob and Alice derived the exact same AES session key
        assertTrue(aliceSessionKey.contentEquals(bobSessionKey))
    }
}

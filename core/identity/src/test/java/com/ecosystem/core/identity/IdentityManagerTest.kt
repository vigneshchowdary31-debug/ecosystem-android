package com.ecosystem.core.identity

import com.ecosystem.core.security.CryptoManager
import com.ecosystem.core.security.SecureStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IdentityManagerTest {

    private val localIdentityDao: LocalIdentityDao = mockk()
    private val cryptoManager: CryptoManager = mockk()
    private val secureStorage: SecureStorage = mockk()
    private lateinit var identityManager: IdentityManager

    @Before
    fun setUp() {
        identityManager = IdentityManagerImpl(localIdentityDao, cryptoManager, secureStorage)
    }

    @Test
    fun `getOrCreateIdentity generates new UUID and keys on first launch`() = runTest {
        // Given
        coEvery { localIdentityDao.getIdentity(any()) } returns null
        every { secureStorage.getString("identity_device_id") } returns null
        every { cryptoManager.getIdentityPublicKey(any()) } returns null

        val uuidSlot = slot<String>()
        every { secureStorage.putString("identity_device_id", capture(uuidSlot)) } returns Unit
        every { cryptoManager.generateIdentityKeyPair("primary_device_identity") } returns "mock_pub_key"
        coEvery { localIdentityDao.insertIdentity(any()) } returns Unit

        // When
        val deviceInfo = identityManager.getOrCreateIdentity()

        // Then
        assertNotNull(deviceInfo)
        assertEquals("mock_pub_key", deviceInfo.publicKeyEd25519)
        assertTrue(uuidSlot.isCaptured)
        assertEquals(uuidSlot.captured, deviceInfo.deviceId)

        verify { secureStorage.putString("identity_device_id", any()) }
        verify { cryptoManager.generateIdentityKeyPair("primary_device_identity") }
        coVerify { localIdentityDao.insertIdentity(any()) }
    }

    @Test
    fun `getOrCreateIdentity returns existing identity on subsequent launches`() = runTest {
        // Given
        val existingUuid = "existing-secure-uuid-1234"
        val existingPubKey = "existing_pub_key"
        val mockEntity = LocalIdentityEntity(
            idAlias = "primary_device_identity",
            deviceId = existingUuid,
            name = "Test Device",
            publicKeyEd25519 = existingPubKey,
            createdAt = 1000L
        )

        coEvery { localIdentityDao.getIdentity("primary_device_identity") } returns mockEntity
        every { secureStorage.getString("identity_device_id") } returns existingUuid
        every { cryptoManager.getIdentityPublicKey("primary_device_identity") } returns existingPubKey

        // When
        val deviceInfo = identityManager.getOrCreateIdentity()

        // Then
        assertNotNull(deviceInfo)
        assertEquals(existingUuid, deviceInfo.deviceId)
        assertEquals(existingPubKey, deviceInfo.publicKeyEd25519)

        verify(exactly = 0) { secureStorage.putString(any(), any()) }
        verify(exactly = 0) { cryptoManager.generateIdentityKeyPair(any()) }
        coVerify(exactly = 0) { localIdentityDao.insertIdentity(any()) }
    }

    @Test
    fun `getIdentity returns null when no identity exists`() = runTest {
        // Given
        coEvery { localIdentityDao.getIdentity(any()) } returns null
        every { secureStorage.getString(any()) } returns null
        every { cryptoManager.getIdentityPublicKey(any()) } returns null

        // When
        val deviceInfo = identityManager.getIdentity()

        // Then
        assertNull(deviceInfo)
    }

    @Test
    fun `signChallenge delegates challenge signing to CryptoManager`() = runTest {
        // Given
        val challenge = "challenge_data".toByteArray()
        val expectedSignature = "signature_data".toByteArray()
        every { cryptoManager.signWithIdentityKey("primary_device_identity", challenge) } returns expectedSignature

        // When
        val signature = identityManager.signChallenge(challenge)

        // Then
        assertEquals(expectedSignature, signature)
        verify { cryptoManager.signWithIdentityKey("primary_device_identity", challenge) }
    }
}

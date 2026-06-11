package com.ecosystem.core.security

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SecureStorageTest {

    private val context: Context = mockk()
    private val keystoreManager: KeystoreManager = mockk()
    private val sharedPreferences: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)

    private lateinit var secureStorage: SecureStorage

    @Before
    fun setUp() {
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        
        secureStorage = SecureStorageImpl(context, keystoreManager)
    }

    @Test
    fun `putString encrypts and saves value`() {
        // Given
        val key = "test_key"
        val rawValue = "raw_value"
        val encryptedValue = "encrypted_value"
        
        every { keystoreManager.encrypt(rawValue) } returns encryptedValue
        every { editor.putString(key, encryptedValue) } returns editor

        // When
        secureStorage.putString(key, rawValue)

        // Then
        verify { keystoreManager.encrypt(rawValue) }
        verify { editor.putString(key, encryptedValue) }
        verify { editor.apply() }
    }

    @Test
    fun `getString retrieves and decrypts value`() {
        // Given
        val key = "test_key"
        val encryptedValue = "encrypted_value"
        val decryptedValue = "decrypted_value"

        every { sharedPreferences.getString(key, null) } returns encryptedValue
        every { keystoreManager.decrypt(encryptedValue) } returns decryptedValue

        // When
        val result = secureStorage.getString(key)

        // Then
        assertEquals(decryptedValue, result)
        verify { sharedPreferences.getString(key, null) }
        verify { keystoreManager.decrypt(encryptedValue) }
    }

    @Test
    fun `getString returns null when key not found`() {
        // Given
        val key = "non_existent_key"
        every { sharedPreferences.getString(key, null) } returns null

        // When
        val result = secureStorage.getString(key)

        // Then
        assertNull(result)
        verify { sharedPreferences.getString(key, null) }
        verify(exactly = 0) { keystoreManager.decrypt(any()) }
    }
}

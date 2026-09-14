package com.ecosystem.core.security

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class SecureStorageTest {

    private val context: Context = mockk()
    private val keystoreManager: KeystoreManager = mockk()
    private val preferences: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)
    private lateinit var storage: SecureStorageImpl

    private val sealed = byteArrayOf(9, 9, 9)
    private val aad = "ecosystem_secure_prefs/test_key".toByteArray()

    @Before
    fun setUp() {
        every { context.getSharedPreferences(any(), any()) } returns preferences
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.commit() } returns true
        storage = SecureStorageImpl(context, keystoreManager)
    }

    @Test
    fun `putBytes seals with key-bound associated data and commits synchronously`() {
        val stored = slot<String>()
        every { keystoreManager.encrypt(any(), any()) } returns sealed
        every { editor.putString("test_key", capture(stored)) } returns editor

        storage.putBytes("test_key", byteArrayOf(1, 2))

        verify { keystoreManager.encrypt(byteArrayOf(1, 2), aad) }
        verify { editor.commit() }
        assertEquals("v2:" + Base64.getEncoder().encodeToString(sealed), stored.captured)
    }

    @Test(expected = IllegalStateException::class)
    fun `failed commit is an error, not silently ignored`() {
        every { keystoreManager.encrypt(any(), any()) } returns sealed
        every { editor.commit() } returns false
        storage.putBytes("test_key", byteArrayOf(1))
    }

    @Test
    fun `v2 values are opened with associated data`() {
        every { preferences.getString("test_key", null) } returns "v2:" + Base64.getEncoder().encodeToString(sealed)
        every { keystoreManager.decrypt(sealed, aad) } returns byteArrayOf(7)

        val read = storage.readBytes("test_key") as SecureRead.Present
        assertArrayEquals(byteArrayOf(7), read.value)
        assertFalse(read.isLegacyFormat)
    }

    @Test
    fun `legacy values are opened without associated data and flagged`() {
        val legacy = Base64.getMimeEncoder().encodeToString(sealed)
        every { preferences.getString("test_key", null) } returns legacy
        every { keystoreManager.decrypt(sealed, null) } returns "text".toByteArray()

        val read = storage.readBytes("test_key") as SecureRead.Present
        assertTrue(read.isLegacyFormat)
        assertEquals("text", storage.getString("test_key"))
    }

    @Test
    fun `missing and undecryptable values are distinguished`() {
        every { preferences.getString("missing", null) } returns null
        every { preferences.getString("test_key", null) } returns "v2:" + Base64.getEncoder().encodeToString(sealed)
        every { keystoreManager.decrypt(any(), any()) } throws KeystoreUnavailableException("key lost")

        assertEquals(SecureRead.Missing, storage.readBytes("missing"))
        assertTrue(storage.readBytes("test_key") is SecureRead.Unreadable)
        assertEquals(null, storage.getString("test_key"))
    }
}

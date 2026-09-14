package com.ecosystem.core.identity

import com.ecosystem.core.common.DispatcherProvider
import com.ecosystem.core.security.CryptoManager
import com.ecosystem.core.security.CryptoManagerImpl
import com.ecosystem.core.security.IdentityKeyState
import com.ecosystem.core.security.IdentityKeyUnavailableException
import com.ecosystem.core.security.SecureRead
import com.ecosystem.core.security.SecureStorage
import com.google.crypto.tink.subtle.Ed25519Verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

class IdentityManagerTest {

    private class MemoryStorage : SecureStorage {
        val values = LinkedHashMap<String, SecureRead>()
        override fun putBytes(key: String, value: ByteArray) {
            synchronized(values) { values[key] = SecureRead.Present(value.copyOf(), false) }
        }
        override fun readBytes(key: String): SecureRead = synchronized(values) {
            when (val read = values[key]) {
                null -> SecureRead.Missing
                is SecureRead.Present -> SecureRead.Present(read.value.copyOf(), read.isLegacyFormat)
                else -> read
            }
        }
        override fun putString(key: String, value: String) = putBytes(key, value.toByteArray())
        override fun getString(key: String) = (readBytes(key) as? SecureRead.Present)?.let { String(it.value) }
        override fun remove(key: String) {
            synchronized(values) { values.remove(key) }
        }
        override fun clear() = synchronized(values) { values.clear() }
    }

    private class MemoryDao : LocalIdentityDao {
        var row: LocalIdentityEntity? = null
        override suspend fun getIdentity(alias: String) = row
        override suspend fun insertIdentity(identity: LocalIdentityEntity) {
            row = identity
        }
        override suspend fun deleteIdentity(alias: String) {
            row = null
        }
    }

    private class CountingCrypto(private val delegate: CryptoManager) : CryptoManager by delegate {
        val generated = AtomicInteger()
        override fun generateIdentityKeyPair(alias: String): ByteArray {
            generated.incrementAndGet()
            Thread.sleep(20)
            return delegate.generateIdentityKeyPair(alias)
        }
    }

    private class TestDispatchers(dispatcher: CoroutineDispatcher) : DispatcherProvider {
        override val main = dispatcher
        override val io = dispatcher
        override val default = dispatcher
        override val unconfined = dispatcher
    }

    private val storage = MemoryStorage()
    private val dao = MemoryDao()
    private val crypto = CountingCrypto(CryptoManagerImpl(storage))

    private fun manager(dispatcher: CoroutineDispatcher = Dispatchers.Unconfined) =
        IdentityManagerImpl(dao, crypto, storage, TestDispatchers(dispatcher))

    @Test
    fun `first launch creates a stable identity and later launches return it`() = runTest {
        val first = manager().getOrCreateIdentity()
        val second = manager().getOrCreateIdentity()
        assertEquals(first, second)
        assertEquals(1, crypto.generated.get())
        assertTrue(first.deviceId.matches(Regex("[0-9a-f-]{36}")))
        assertEquals(32, Base64.getDecoder().decode(first.publicKeyEd25519).size)
    }

    @Test
    fun `concurrent first launches create exactly one identity`() {
        val identityManager = manager(Dispatchers.Default)
        val identities = runBlocking {
            (1..8).map { async(Dispatchers.Default) { identityManager.getOrCreateIdentity() } }.awaitAll()
        }
        assertEquals(1, identities.toSet().size)
        assertEquals(1, crypto.generated.get())
    }

    @Test
    fun `losing the database row does not change the identity`() = runTest {
        val original = manager().getOrCreateIdentity()
        dao.row = null
        val afterWipe = manager().getOrCreateIdentity()
        assertEquals(original.deviceId, afterWipe.deviceId)
        assertEquals(original.publicKeyEd25519, afterWipe.publicKeyEd25519)
        assertEquals(original.advertisingIdentifier, afterWipe.advertisingIdentifier)
    }

    @Test
    fun `unreadable identity material fails loudly instead of rotating`() = runTest {
        manager().getOrCreateIdentity()
        storage.values["identity_device_id"] = SecureRead.Unreadable(IllegalStateException("Keystore key lost"))
        try {
            manager().getOrCreateIdentity()
            fail("Expected IdentityKeyUnavailableException")
        } catch (expected: IdentityKeyUnavailableException) {
        }
        assertTrue(storage.values["identity_device_id"] is SecureRead.Unreadable)
        assertEquals(1, crypto.generated.get())
    }

    @Test
    fun `unreadable private key fails loudly`() = runTest {
        manager().getOrCreateIdentity()
        storage.values["primary_device_identity_prv"] = SecureRead.Unreadable(IllegalStateException("lost"))
        assertTrue(crypto.identityKeyState("primary_device_identity") is IdentityKeyState.Unreadable)
        try {
            manager().getOrCreateIdentity()
            fail("Expected IdentityKeyUnavailableException")
        } catch (expected: IdentityKeyUnavailableException) {
        }
    }

    @Test
    fun `legacy device id text is kept and migrated`() = runTest {
        val legacyId = "87654321-4321-4321-4321-210987654321"
        storage.values["identity_device_id"] = SecureRead.Present(legacyId.toByteArray(), isLegacyFormat = true)
        val identity = manager().getOrCreateIdentity()
        assertEquals(legacyId, identity.deviceId)
        assertEquals(false, (storage.values["identity_device_id"] as SecureRead.Present).isLegacyFormat)
    }

    @Test
    fun `signatures verify with the published public key`() = runTest {
        val identityManager = manager()
        val identity = identityManager.getOrCreateIdentity()
        val data = "authentication input".toByteArray()
        Ed25519Verify(Base64.getDecoder().decode(identity.publicKeyEd25519)).verify(identityManager.sign(data), data)
    }

    @Test
    fun `getIdentity does not create an identity`() = runTest {
        assertNull(manager().getIdentity())
        assertEquals(0, crypto.generated.get())
    }

    @Test
    fun `reset creates a new identity`() = runTest {
        val identityManager = manager()
        val original = identityManager.getOrCreateIdentity()
        val reset = identityManager.resetIdentity()
        assertNotEquals(original.deviceId, reset.deviceId)
        assertNotEquals(original.publicKeyEd25519, reset.publicKeyEd25519)
        assertEquals(reset, identityManager.getOrCreateIdentity())
    }
}

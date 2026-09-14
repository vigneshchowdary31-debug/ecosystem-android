package com.ecosystem.core.trusteddevices

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class TrustedDeviceRepositoryTest {

    private class MemoryDao : TrustedDeviceDao() {
        val rows = MutableStateFlow<Map<String, TrustedDeviceEntity>>(emptyMap())

        private fun active() = rows.value.values.filter { it.isTrustActive }.sortedByDescending { it.pairingTimestamp }

        override fun observeActive(): Flow<List<TrustedDeviceEntity>> = rows.map { active() }
        override suspend fun getActive() = active()
        override suspend fun getById(deviceId: String) = rows.value[deviceId]
        override suspend fun insertOrReplace(device: TrustedDeviceEntity) {
            rows.value = rows.value + (device.deviceId to device)
        }
        override suspend fun recordSeen(deviceId: String, timestamp: Long, ipAddress: String?): Int {
            val row = rows.value[deviceId] ?: return 0
            insertOrReplace(row.copy(lastSeenTimestamp = timestamp, lastKnownIpAddress = ipAddress ?: row.lastKnownIpAddress))
            return 1
        }
        override suspend fun revoke(deviceId: String): Int {
            val row = rows.value[deviceId] ?: return 0
            insertOrReplace(row.copy(isTrustActive = false))
            return 1
        }
        override suspend fun delete(deviceId: String): Int {
            val had = deviceId in rows.value
            rows.value = rows.value - deviceId
            return if (had) 1 else 0
        }
    }

    private val dao = MemoryDao()
    private val repository = TrustedDeviceRepositoryImpl(dao)
    private val macId = "2D861D8A-6B83-4A1D-8F9C-76E48C3F8F11"
    private val canonicalMacId = macId.lowercase()
    private val keyA = ByteArray(32) { 1 }
    private val keyB = ByteArray(32) { 2 }

    @Test
    fun `paired device is stored under its canonical id with its key`() = runTest {
        repository.savePairedDevice(macId, "Studio Mac", keyA, "192.168.1.10", 1_000)
        val device = repository.getDevice(macId)!!
        assertEquals(canonicalMacId, device.deviceId)
        assertEquals(Base64.getEncoder().encodeToString(keyA), device.publicKeyEd25519)
        assertEquals("192.168.1.10", device.lastKnownIpAddress)
        assertEquals(1_000, device.pairedAt)
        assertTrue(device.isTrustActive)
    }

    @Test
    fun `re-pairing with the same key keeps the first pairing time and refreshes the rest`() = runTest {
        repository.savePairedDevice(macId, "Studio Mac", keyA, "192.168.1.10", 1_000)
        repository.savePairedDevice(macId, "Studio Mac (renamed)", keyA, null, 5_000)
        val device = repository.getDevice(macId)!!
        assertEquals(1_000, device.pairedAt)
        assertEquals(5_000, device.lastSeenAt)
        assertEquals("Studio Mac (renamed)", device.name)
        assertEquals("192.168.1.10", device.lastKnownIpAddress)
        assertEquals(1, repository.getTrustedDevices().size)
    }

    @Test
    fun `re-pairing with a new key starts a new trust relationship`() = runTest {
        repository.savePairedDevice(macId, "Studio Mac", keyA, null, 1_000)
        repository.savePairedDevice(macId, "Studio Mac", keyB, null, 9_000)
        val device = repository.getDevice(macId)!!
        assertEquals(Base64.getEncoder().encodeToString(keyB), device.publicKeyEd25519)
        assertEquals(9_000, device.pairedAt)
    }

    @Test
    fun `multiple macs are kept, newest first`() = runTest {
        repository.savePairedDevice(macId, "Studio Mac", keyA, null, 1_000)
        repository.savePairedDevice("a0000000-0000-4000-8000-000000000001", "MacBook", keyB, null, 2_000)
        assertEquals(listOf("MacBook", "Studio Mac"), repository.observeTrustedDevices().first().map { it.name })
    }

    @Test
    fun `malformed rows are never returned as trusted`() = runTest {
        val valid = TrustedDeviceEntity(canonicalMacId, "Mac", null, Base64.getEncoder().encodeToString(keyA), true, 1, 1)
        dao.insertOrReplace(valid)
        dao.insertOrReplace(valid.copy(deviceId = "b0000000-0000-4000-8000-000000000002", publicKeyEd25519 = "mock_pubkey_bytes_1234"))
        dao.insertOrReplace(valid.copy(deviceId = "not-a-uuid"))
        dao.insertOrReplace(valid.copy(deviceId = "c0000000-0000-4000-8000-000000000003", name = " "))
        dao.insertOrReplace(valid.copy(deviceId = "D0000000-0000-4000-8000-000000000004"))
        assertEquals(listOf(canonicalMacId), repository.getTrustedDevices().map { it.deviceId })
    }

    @Test
    fun `last seen updates keep the address when none is known`() = runTest {
        repository.savePairedDevice(macId, "Studio Mac", keyA, "192.168.1.10", 1_000)
        repository.recordSeen(macId, 7_000, null)
        assertEquals("192.168.1.10", repository.getDevice(macId)!!.lastKnownIpAddress)
        repository.recordSeen(macId, 8_000, "192.168.1.11")
        val device = repository.getDevice(macId)!!
        assertEquals("192.168.1.11", device.lastKnownIpAddress)
        assertEquals(8_000, device.lastSeenAt)
    }

    @Test
    fun `revoked devices are hidden until paired again`() = runTest {
        repository.savePairedDevice(macId, "Studio Mac", keyA, null, 1_000)
        repository.revoke(macId)
        assertTrue(repository.getTrustedDevices().isEmpty())
        repository.savePairedDevice(macId, "Studio Mac", keyA, null, 2_000)
        assertEquals(1, repository.getTrustedDevices().size)
    }

    @Test
    fun `delete removes the device`() = runTest {
        repository.savePairedDevice(macId, "Studio Mac", keyA, null, 1_000)
        repository.delete(macId)
        assertNull(repository.getDevice(macId))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid keys are refused`() = runTest {
        repository.savePairedDevice(macId, "Studio Mac", ByteArray(31), null, 1_000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid ids are refused`() = runTest {
        repository.savePairedDevice("AA:BB:CC:DD:EE:FF", "Studio Mac", keyA, null, 1_000)
    }
}

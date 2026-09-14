package com.ecosystem.core.networking.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.ecosystem.core.common.BleConstants
import com.ecosystem.core.pairing.ConnectionPhase
import com.ecosystem.core.pairing.TransportException
import com.ecosystem.core.protocol.framing.Frame
import com.ecosystem.core.protocol.framing.FrameCodec
import com.ecosystem.core.protocol.framing.Fragmenter
import com.ecosystem.core.protocol.link.LinkException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Drives BleGattConnection through mocked Android GATT objects. Unit tests see SDK_INT = 0,
 * so the pre-API-33 write paths are exercised. Physical BLE behaviour still needs devices.
 */
class BleGattConnectionTest {

    private val context = mockk<Context>(relaxed = true)
    private val device = mockk<BluetoothDevice>()
    private val gatt = mockk<BluetoothGatt>(relaxed = true)
    private val service = mockk<BluetoothGattService>()
    private val writeCharacteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
    private val notifyCharacteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
    private val cccd = mockk<BluetoothGattDescriptor>(relaxed = true)

    private lateinit var callback: BluetoothGattCallback
    private var connectStatus = BluetoothGatt.GATT_SUCCESS
    private var connectCallbackFires = true
    private var mtuToReport: Int? = 185
    private var writeStatus = BluetoothGatt.GATT_SUCCESS
    private var pendingValue: ByteArray = ByteArray(0)
    private val writes = mutableListOf<ByteArray>()
    private val phases = mutableListOf<ConnectionPhase>()

    @Before
    fun setUp() {
        every { context.applicationContext } returns context
        every { device.connectGatt(any(), any(), any(), any<Int>()) } answers {
            callback = thirdArg()
            if (connectCallbackFires) {
                val state = if (connectStatus == BluetoothGatt.GATT_SUCCESS) BluetoothProfile.STATE_CONNECTED else BluetoothProfile.STATE_DISCONNECTED
                callback.onConnectionStateChange(gatt, connectStatus, state)
            }
            gatt
        }
        every { gatt.requestMtu(any()) } answers {
            mtuToReport?.let { callback.onMtuChanged(gatt, it, BluetoothGatt.GATT_SUCCESS) }
            mtuToReport != null
        }
        every { gatt.discoverServices() } answers {
            callback.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)
            true
        }
        every { gatt.getService(BleConstants.SERVICE_UUID) } returns service
        every { service.getCharacteristic(BleConstants.WRITE_CHAR_UUID) } returns writeCharacteristic
        every { service.getCharacteristic(BleConstants.NOTIFY_CHAR_UUID) } returns notifyCharacteristic
        every { writeCharacteristic.properties } returns BluetoothGattCharacteristic.PROPERTY_WRITE
        every { notifyCharacteristic.properties } returns BluetoothGattCharacteristic.PROPERTY_NOTIFY
        every { notifyCharacteristic.uuid } returns BleConstants.NOTIFY_CHAR_UUID
        every { notifyCharacteristic.getDescriptor(BleConstants.CCC_DESCRIPTOR_UUID) } returns cccd
        every { gatt.setCharacteristicNotification(notifyCharacteristic, true) } returns true
        every { gatt.writeDescriptor(cccd) } answers {
            callback.onDescriptorWrite(gatt, cccd, BluetoothGatt.GATT_SUCCESS)
            true
        }
        every { writeCharacteristic.setValue(any<ByteArray>()) } answers {
            pendingValue = firstArg()
            true
        }
        every { gatt.writeCharacteristic(writeCharacteristic) } answers {
            writes += pendingValue
            callback.onCharacteristicWrite(gatt, writeCharacteristic, writeStatus)
            true
        }
    }

    private suspend fun open() = BleGattConnection.open(context, device, { phases += it })

    private fun notify(bytes: ByteArray) = callback.onCharacteristicChanged(gatt, notifyCharacteristic, bytes)

    @Test
    fun `opens in order and uses the negotiated mtu`() = runTest {
        val connection = open()
        assertEquals(
            listOf(
                ConnectionPhase.CONNECTING,
                ConnectionPhase.NEGOTIATING_MTU,
                ConnectionPhase.DISCOVERING_SERVICES,
                ConnectionPhase.ENABLING_NOTIFICATIONS,
                ConnectionPhase.READY,
            ),
            phases,
        )
        assertEquals(185, connection.negotiatedMtu)
        assertEquals(182, connection.maxPayloadPerPacket)
        connection.close()
    }

    @Test
    fun `without an mtu exchange every write fits the default 20 byte payload`() = runTest {
        mtuToReport = null
        val connection = open()
        assertEquals(20, connection.maxPayloadPerPacket)

        val frame = Frame(0x03, ByteArray(300) { it.toByte() })
        connection.send(frame)
        assertTrue(writes.all { it.size <= 20 })
        assertEquals(FrameCodec.encode(frame).toList(), writes.flatMap { it.toList() })
        connection.close()
    }

    @Test
    fun `large mtu sends each frame in few packets`() = runTest {
        mtuToReport = 517
        val connection = open()
        connection.send(Frame(0x03, ByteArray(600)))
        assertEquals(2, writes.size)
        assertTrue(writes.all { it.size <= 512 })
        connection.close()
    }

    @Test
    fun `fragmented notifications are reassembled by length`() = runTest {
        val connection = open()
        val frame = Frame(0x04, ByteArray(170) { (it * 3).toByte() })
        Fragmenter.split(FrameCodec.encode(frame), 7).forEach(::notify)
        assertEquals(frame, connection.receive(1_000))
        connection.close()
    }

    @Test
    fun `several frames in one notification are all delivered`() = runTest {
        val connection = open()
        val first = Frame(0x02, ByteArray(64) { 1 })
        val second = Frame(0x06, ByteArray(32) { 2 })
        notify(FrameCodec.encode(first) + FrameCodec.encode(second))
        assertEquals(first, connection.receive(1_000))
        assertEquals(second, connection.receive(1_000))
        connection.close()
    }

    @Test
    fun `invalid bytes from the peer fail the link`() = runTest {
        val connection = open()
        notify("auth_resp:legacy".toByteArray())
        try {
            connection.receive(1_000)
            fail("Expected LinkException")
        } catch (e: LinkException) {
            assertEquals(LinkException.Kind.FRAMING_ERROR, e.kind)
        }
        connection.close()
    }

    @Test
    fun `missing service is reported and the gatt is closed`() = runTest {
        every { gatt.getService(BleConstants.SERVICE_UUID) } returns null
        expectTransportFailure(TransportException.Kind.SERVICE_NOT_FOUND)
        verify(exactly = 1) { gatt.close() }
    }

    @Test
    fun `missing characteristic is reported and the gatt is closed`() = runTest {
        every { service.getCharacteristic(BleConstants.NOTIFY_CHAR_UUID) } returns null
        expectTransportFailure(TransportException.Kind.CHARACTERISTIC_MISSING)
        verify(exactly = 1) { gatt.close() }
    }

    @Test
    fun `gatt 133 on connect is a transient failure and the gatt is closed`() = runTest {
        connectStatus = 133
        val failure = expectTransportFailure(TransportException.Kind.CONNECT_FAILED)
        assertTrue(failure.transient)
        verify(exactly = 1) { gatt.close() }
    }

    @Test
    fun `connection that never completes times out and is closed`() = runTest {
        connectCallbackFires = false
        val failure = expectTransportFailure(TransportException.Kind.TIMEOUT)
        assertTrue(failure.transient)
        verify(exactly = 1) { gatt.close() }
    }

    @Test
    fun `failed notification subscription is reported`() = runTest {
        every { gatt.writeDescriptor(cccd) } answers {
            callback.onDescriptorWrite(gatt, cccd, BluetoothGatt.GATT_FAILURE)
            true
        }
        expectTransportFailure(TransportException.Kind.NOTIFICATION_SETUP_FAILED)
    }

    @Test
    fun `permission revoked while connecting is reported as permission denied`() = runTest {
        every { device.connectGatt(any(), any(), any(), any<Int>()) } throws SecurityException("BLUETOOTH_CONNECT")
        expectTransportFailure(TransportException.Kind.PERMISSION_DENIED)
    }

    @Test
    fun `disconnect while waiting for a message fails the receive`() = runTest {
        val connection = open()
        callback.onConnectionStateChange(gatt, 19, BluetoothProfile.STATE_DISCONNECTED)
        try {
            connection.receive(1_000)
            fail("Expected LinkException")
        } catch (e: LinkException) {
            assertEquals(LinkException.Kind.DISCONNECTED, e.kind)
        }
        connection.close()
    }

    @Test
    fun `failed write is reported`() = runTest {
        val connection = open()
        writeStatus = BluetoothGatt.GATT_FAILURE
        try {
            connection.send(Frame(0x05, ByteArray(10)))
            fail("Expected LinkException")
        } catch (e: LinkException) {
            assertEquals(LinkException.Kind.WRITE_FAILED, e.kind)
        }
        connection.close()
    }

    @Test
    fun `receive times out when the peer is silent`() = runTest {
        val connection = open()
        try {
            connection.receive(5_000)
            fail("Expected LinkException")
        } catch (e: LinkException) {
            assertEquals(LinkException.Kind.TIMEOUT, e.kind)
        }
        connection.close()
    }

    @Test
    fun `close is idempotent and later sends fail`() = runTest {
        val connection = open()
        connection.close()
        connection.close()
        verify(exactly = 1) { gatt.close() }
        try {
            connection.send(Frame(0x05, ByteArray(1)))
            fail("Expected LinkException")
        } catch (e: LinkException) {
            assertEquals(LinkException.Kind.DISCONNECTED, e.kind)
        }
    }

    private suspend fun expectTransportFailure(kind: TransportException.Kind): TransportException {
        try {
            open()
            fail("Expected TransportException($kind)")
            throw AssertionError()
        } catch (e: TransportException) {
            assertEquals(kind, e.kind)
            return e
        }
    }
}

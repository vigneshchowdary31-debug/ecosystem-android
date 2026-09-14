package com.ecosystem.core.networking.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import com.ecosystem.core.common.BleConstants
import com.ecosystem.core.common.log.AppLog
import com.ecosystem.core.pairing.ConnectionPhase
import com.ecosystem.core.pairing.PairingConnection
import com.ecosystem.core.pairing.TransportException
import com.ecosystem.core.protocol.framing.AttMtu
import com.ecosystem.core.protocol.framing.Fragmenter
import com.ecosystem.core.protocol.framing.Frame
import com.ecosystem.core.protocol.framing.FrameCodec
import com.ecosystem.core.protocol.framing.FrameReassembler
import com.ecosystem.core.protocol.framing.FramingException
import com.ecosystem.core.protocol.link.LinkException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class BleTimeouts(
    val connectMillis: Long = 10_000,
    val mtuMillis: Long = 3_000,
    val discoveryMillis: Long = 10_000,
    val descriptorWriteMillis: Long = 5_000,
    val writeMillis: Long = 5_000,
)

/**
 * One GATT client connection to one candidate, used by exactly one pairing session.
 *
 * - GATT operations are serialised (Android allows one outstanding operation per connection)
 *   and every operation has a timeout.
 * - Outgoing frames are cut into packets of `negotiated ATT_MTU - 3` bytes, one Write Request
 *   each; incoming notifications are reassembled by frame length. No MTU is ever assumed: if
 *   the MTU exchange fails the default of 23 (20-byte packets) is used.
 * - Any failure, timeout, disconnect or cancellation ends in [close], which disconnects and
 *   closes the [BluetoothGatt].
 */
@SuppressLint("MissingPermission")
class BleGattConnection private constructor(
    private val context: Context,
    private val device: BluetoothDevice,
    private val timeouts: BleTimeouts,
) : PairingConnection {

    private sealed interface GattEvent {
        data class ConnectionChanged(val status: Int, val newState: Int) : GattEvent
        data class MtuChanged(val mtu: Int, val status: Int) : GattEvent
        data class ServicesDiscovered(val status: Int) : GattEvent
        data class DescriptorWritten(val status: Int) : GattEvent
        data class CharacteristicWritten(val status: Int) : GattEvent
    }

    private class PendingOperation(val accepts: (GattEvent) -> Boolean) {
        val result = CompletableDeferred<GattEvent>()
    }

    private class GattOperationException(val kind: Kind, message: String) : Exception(message) {
        enum class Kind { NOT_STARTED, TIMEOUT, DISCONNECTED, PERMISSION_DENIED }
    }

    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var writeCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile var negotiatedMtu: Int = AttMtu.DEFAULT_ATT_MTU
        private set

    val maxPayloadPerPacket: Int get() = AttMtu.maxPayloadPerPacket(negotiatedMtu)

    private val closed = AtomicBoolean(false)
    private val operationMutex = Mutex()
    private val pending = AtomicReference<PendingOperation?>(null)
    private val incoming = Channel<Frame>(capacity = INCOMING_CAPACITY)
    private val reassembler = FrameReassembler()

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val event = GattEvent.ConnectionChanged(status, newState)
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                AppLog.d(TAG) { "GATT disconnected, status=$status" }
                pending.getAndSet(null)?.result?.complete(event)
                incoming.close(LinkException(LinkException.Kind.DISCONNECTED, "Peer disconnected (GATT status $status)"))
            } else {
                deliver(event)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) =
            deliver(GattEvent.MtuChanged(mtu, status))

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) =
            deliver(GattEvent.ServicesDiscovered(status))

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) =
            deliver(GattEvent.DescriptorWritten(status))

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) =
            deliver(GattEvent.CharacteristicWritten(status))

        // Called below API 33. On API 33+ the framework calls the three-argument overload instead.
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            if (characteristic.uuid == BleConstants.NOTIFY_CHAR_UUID) onNotification(value.copyOf())
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == BleConstants.NOTIFY_CHAR_UUID) onNotification(value)
        }
    }

    /** Completes the pending operation only with the event type it is waiting for, so late callbacks from a timed-out operation are dropped. */
    private fun deliver(event: GattEvent) {
        val operation = pending.get() ?: return
        if (operation.accepts(event) && pending.compareAndSet(operation, null)) {
            operation.result.complete(event)
        }
    }

    private fun onNotification(value: ByteArray) {
        if (closed.get()) return
        val frames = try {
            synchronized(reassembler) { reassembler.append(value) }
        } catch (e: FramingException) {
            AppLog.w(TAG, "Rejected invalid frame from peer: ${e.reason}")
            incoming.close(LinkException(LinkException.Kind.FRAMING_ERROR, "Invalid frame from peer (${e.reason})", e))
            return
        }
        for (frame in frames) {
            if (incoming.trySend(frame).isFailure) {
                incoming.close(LinkException(LinkException.Kind.FRAMING_ERROR, "Peer sent more messages than the protocol allows"))
                return
            }
        }
    }

    private suspend fun establish(onPhase: (ConnectionPhase) -> Unit) {
        try {
            onPhase(ConnectionPhase.CONNECTING)
            val connection = operationMutex.withLock {
                val operation = PendingOperation { it is GattEvent.ConnectionChanged }
                pending.set(operation)
                try {
                    val opened = try {
                        device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                    } catch (e: SecurityException) {
                        throw GattOperationException(GattOperationException.Kind.PERMISSION_DENIED, "BLUETOOTH_CONNECT not granted")
                    } ?: throw GattOperationException(GattOperationException.Kind.NOT_STARTED, "connectGatt returned null")
                    gatt = opened
                    withTimeoutOrNull(timeouts.connectMillis) { operation.result.await() }
                        ?: throw GattOperationException(GattOperationException.Kind.TIMEOUT, "Connection timed out")
                } finally {
                    pending.compareAndSet(operation, null)
                }
            } as GattEvent.ConnectionChanged
            if (connection.status != BluetoothGatt.GATT_SUCCESS || connection.newState != BluetoothProfile.STATE_CONNECTED) {
                // Status 133 (GATT_ERROR) is common on first connection and worth one retry.
                throw TransportException(
                    TransportException.Kind.CONNECT_FAILED,
                    "GATT connection failed (status ${connection.status})",
                    transient = true,
                )
            }

            onPhase(ConnectionPhase.NEGOTIATING_MTU)
            negotiatedMtu = negotiateMtu()
            AppLog.d(TAG) { "ATT MTU $negotiatedMtu, $maxPayloadPerPacket bytes per packet" }

            onPhase(ConnectionPhase.DISCOVERING_SERVICES)
            val discovery = operation<GattEvent.ServicesDiscovered>("discoverServices", timeouts.discoveryMillis) {
                it.discoverServices()
            }
            if (discovery.status != BluetoothGatt.GATT_SUCCESS) {
                throw TransportException(TransportException.Kind.CONNECT_FAILED, "Service discovery failed (status ${discovery.status})", transient = true)
            }
            val service = requireGatt().getService(BleConstants.SERVICE_UUID)
                ?: throw TransportException(TransportException.Kind.SERVICE_NOT_FOUND, "Pairing service not found")
            val write = service.getCharacteristic(BleConstants.WRITE_CHAR_UUID)
                ?.takeIf { it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 }
                ?: throw TransportException(TransportException.Kind.CHARACTERISTIC_MISSING, "Write characteristic missing")
            val notify = service.getCharacteristic(BleConstants.NOTIFY_CHAR_UUID)
                ?.takeIf { it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 }
                ?: throw TransportException(TransportException.Kind.CHARACTERISTIC_MISSING, "Notify characteristic missing")
            writeCharacteristic = write

            onPhase(ConnectionPhase.ENABLING_NOTIFICATIONS)
            if (!requireGatt().setCharacteristicNotification(notify, true)) {
                throw TransportException(TransportException.Kind.NOTIFICATION_SETUP_FAILED, "Could not enable notifications")
            }
            val cccd = notify.getDescriptor(BleConstants.CCC_DESCRIPTOR_UUID)
                ?: throw TransportException(TransportException.Kind.NOTIFICATION_SETUP_FAILED, "CCCD missing")
            val subscribed = operation<GattEvent.DescriptorWritten>("writeDescriptor", timeouts.descriptorWriteMillis) {
                writeDescriptorCompat(it, cccd, ENABLE_NOTIFICATIONS)
            }
            if (subscribed.status != BluetoothGatt.GATT_SUCCESS) {
                throw TransportException(TransportException.Kind.NOTIFICATION_SETUP_FAILED, "CCCD write failed (status ${subscribed.status})")
            }
            onPhase(ConnectionPhase.READY)
        } catch (e: GattOperationException) {
            throw when (e.kind) {
                GattOperationException.Kind.PERMISSION_DENIED ->
                    TransportException(TransportException.Kind.PERMISSION_DENIED, e.message ?: "Permission denied")
                GattOperationException.Kind.TIMEOUT ->
                    TransportException(TransportException.Kind.TIMEOUT, e.message ?: "Timed out", transient = true)
                GattOperationException.Kind.DISCONNECTED ->
                    TransportException(TransportException.Kind.DISCONNECTED, e.message ?: "Disconnected", transient = true)
                GattOperationException.Kind.NOT_STARTED ->
                    TransportException(TransportException.Kind.CONNECT_FAILED, e.message ?: "Operation not started", transient = true)
            }
        }
    }

    /** Best effort: a failed or missing MTU exchange falls back to the default ATT MTU. */
    private suspend fun negotiateMtu(): Int = try {
        val event = operation<GattEvent.MtuChanged>("requestMtu", timeouts.mtuMillis) {
            it.requestMtu(AttMtu.REQUESTED_ATT_MTU)
        }
        if (event.status == BluetoothGatt.GATT_SUCCESS) event.mtu else AttMtu.DEFAULT_ATT_MTU
    } catch (e: GattOperationException) {
        if (e.kind == GattOperationException.Kind.DISCONNECTED || e.kind == GattOperationException.Kind.PERMISSION_DENIED) throw e
        AppLog.d(TAG) { "MTU exchange unavailable (${e.kind}); using the default" }
        AttMtu.DEFAULT_ATT_MTU
    }

    private suspend inline fun <reified T : GattEvent> operation(
        name: String,
        timeoutMillis: Long,
        crossinline start: (BluetoothGatt) -> Boolean,
    ): T = operationMutex.withLock {
        val target = gatt
        if (closed.get() || target == null) {
            throw GattOperationException(GattOperationException.Kind.DISCONNECTED, "$name on a closed connection")
        }
        val operation = PendingOperation { it is T }
        pending.set(operation)
        try {
            val started = try {
                start(target)
            } catch (e: SecurityException) {
                throw GattOperationException(GattOperationException.Kind.PERMISSION_DENIED, "$name: permission denied")
            }
            if (!started) throw GattOperationException(GattOperationException.Kind.NOT_STARTED, "$name could not be started")
            val event = withTimeoutOrNull(timeoutMillis) { operation.result.await() }
                ?: throw GattOperationException(GattOperationException.Kind.TIMEOUT, "$name timed out")
            if (event is GattEvent.ConnectionChanged) {
                throw GattOperationException(GattOperationException.Kind.DISCONNECTED, "Disconnected during $name")
            }
            event as T
        } finally {
            pending.compareAndSet(operation, null)
        }
    }

    override suspend fun send(frame: Frame) {
        val characteristic = writeCharacteristic
        if (closed.get() || characteristic == null) {
            throw LinkException(LinkException.Kind.DISCONNECTED, "Connection is closed")
        }
        for (packet in Fragmenter.split(FrameCodec.encode(frame), maxPayloadPerPacket)) {
            val written = try {
                operation<GattEvent.CharacteristicWritten>("writeCharacteristic", timeouts.writeMillis) {
                    writeCharacteristicCompat(it, characteristic, packet)
                }
            } catch (e: GattOperationException) {
                throw when (e.kind) {
                    GattOperationException.Kind.DISCONNECTED -> LinkException(LinkException.Kind.DISCONNECTED, e.message ?: "Disconnected")
                    GattOperationException.Kind.TIMEOUT -> LinkException(LinkException.Kind.TIMEOUT, e.message ?: "Write timed out")
                    GattOperationException.Kind.PERMISSION_DENIED -> LinkException(LinkException.Kind.PERMISSION_DENIED, e.message ?: "Permission denied")
                    GattOperationException.Kind.NOT_STARTED -> LinkException(LinkException.Kind.WRITE_FAILED, e.message ?: "Write not started")
                }
            }
            if (written.status != BluetoothGatt.GATT_SUCCESS) {
                throw LinkException(LinkException.Kind.WRITE_FAILED, "Write failed (status ${written.status})")
            }
        }
    }

    override suspend fun receive(timeoutMillis: Long): Frame {
        val result = withTimeoutOrNull(timeoutMillis) { incoming.receiveCatching() }
            ?: throw LinkException(LinkException.Kind.TIMEOUT, "No message from peer within $timeoutMillis ms")
        if (result.isSuccess) return result.getOrThrow()
        val cause = result.exceptionOrNull()
        throw cause as? LinkException ?: LinkException(LinkException.Kind.DISCONNECTED, "Connection closed", cause)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        pending.getAndSet(null)?.result?.complete(
            GattEvent.ConnectionChanged(BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
        )
        incoming.close(LinkException(LinkException.Kind.DISCONNECTED, "Connection closed locally"))
        val target = gatt
        gatt = null
        writeCharacteristic = null
        if (target != null) {
            try {
                target.disconnect()
            } catch (ignored: SecurityException) {
            }
            try {
                target.close()
            } catch (ignored: SecurityException) {
            }
        }
        AppLog.d(TAG) { "GATT connection closed" }
    }

    private fun requireGatt(): BluetoothGatt =
        gatt ?: throw GattOperationException(GattOperationException.Kind.DISCONNECTED, "Connection closed")

    @Suppress("DEPRECATION")
    private fun writeDescriptorCompat(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = value
            gatt.writeDescriptor(descriptor)
        }

    @Suppress("DEPRECATION")
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = value
            gatt.writeCharacteristic(characteristic)
        }

    companion object {
        private const val TAG = "BleGattConnection"
        private const val INCOMING_CAPACITY = 16

        /** CCCD value enabling notifications (0x0001, little-endian). */
        private val ENABLE_NOTIFICATIONS = byteArrayOf(0x01, 0x00)

        /**
         * Connects and prepares the link: connect, MTU exchange, service discovery and
         * notification subscription. On any failure the connection is closed before the
         * exception propagates, including cancellation.
         */
        suspend fun open(
            context: Context,
            device: BluetoothDevice,
            onPhase: (ConnectionPhase) -> Unit,
            timeouts: BleTimeouts = BleTimeouts(),
        ): BleGattConnection {
            val connection = BleGattConnection(context.applicationContext, device, timeouts)
            try {
                connection.establish(onPhase)
                return connection
            } catch (t: Throwable) {
                connection.close()
                throw t
            }
        }
    }
}

package com.ecosystem.core.networking

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ecosystem.core.common.BleConstants
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class BleChannelImpl @Inject constructor(
    private val context: Context
) : BleChannel {

    companion object {
        val SERVICE_UUID: UUID = BleConstants.SERVICE_UUID
        val WRITE_CHAR_UUID: UUID = BleConstants.WRITE_CHAR_UUID
        val NOTIFY_CHAR_UUID: UUID = BleConstants.NOTIFY_CHAR_UUID
        val CCC_DESCRIPTOR_UUID: UUID = BleConstants.CCC_DESCRIPTOR_UUID
    }

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _incomingPackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingPackets: SharedFlow<ByteArray> = _incomingPackets.asSharedFlow()

    private val _state = MutableStateFlow<ChannelState>(ChannelState.Disconnected)
    override val state: StateFlow<ChannelState> = _state.asStateFlow()

    private var connectionCallback: ((Boolean) -> Unit)? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.value = ChannelState.Error("GATT Error status: $status")
                connectionCallback?.invoke(false)
                connectionCallback = null
                disconnect()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.value = ChannelState.Connecting
                android.util.Log.d("BLE_ADVERTISE_DEBUG", "Connected to GATT Server. Requesting MTU 512...")
                gatt.requestMtu(512)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _state.value = ChannelState.Disconnected
                connectionCallback?.invoke(false)
                connectionCallback = null
                bluetoothGatt = null
                writeCharacteristic = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            android.util.Log.d("BLE_ADVERTISE_DEBUG", "onMtuChanged: negotiated MTU is $mtu, status=$status")
            // Proceed to discover services after MTU negotiation
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.value = ChannelState.Error("Service Discovery Failed")
                connectionCallback?.invoke(false)
                connectionCallback = null
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                _state.value = ChannelState.Error("Continuity Service Not Found")
                connectionCallback?.invoke(false)
                connectionCallback = null
                return
            }

            writeCharacteristic = service.getCharacteristic(WRITE_CHAR_UUID)
            val notifyChar = service.getCharacteristic(NOTIFY_CHAR_UUID)

            if (writeCharacteristic == null || notifyChar == null) {
                _state.value = ChannelState.Error("Characteristics Not Found")
                connectionCallback?.invoke(false)
                connectionCallback = null
                return
            }

            // Enable Notifications on Read/Notify Characteristic
            gatt.setCharacteristicNotification(notifyChar, true)
            val descriptor = notifyChar.getDescriptor(CCC_DESCRIPTOR_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val success = gatt.writeDescriptor(descriptor)
                if (!success) {
                    _state.value = ChannelState.Error("Failed to write CCC descriptor")
                    connectionCallback?.invoke(false)
                    connectionCallback = null
                }
            } else {
                _state.value = ChannelState.Connected
                connectionCallback?.invoke(true)
                connectionCallback = null
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            android.util.Log.d("BLE_ADVERTISE_DEBUG", "onDescriptorWrite status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _state.value = ChannelState.Connected
                connectionCallback?.invoke(true)
            } else {
                _state.value = ChannelState.Error("Descriptor write failed status=$status")
                connectionCallback?.invoke(false)
            }
            connectionCallback = null
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == NOTIFY_CHAR_UUID) {
                _incomingPackets.tryEmit(characteristic.value)
            }
        }

        // Support newer API levels where onCharacteristicChanged includes byte value parameter
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == NOTIFY_CHAR_UUID) {
                _incomingPackets.tryEmit(value)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(macAddress: String): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) return false
        
        disconnect()

        val device = bluetoothAdapter!!.getRemoteDevice(macAddress) ?: return false
        _state.value = ChannelState.Connecting

        return suspendCancellableCoroutine<Boolean> { continuation ->
            connectionCallback = { success ->
                if (continuation.isActive) {
                    continuation.resume(success)
                }
            }

            bluetoothGatt = device.connectGatt(context, false, gattCallback)

            continuation.invokeOnCancellation {
                disconnect()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun send(data: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val char = writeCharacteristic ?: return false

        char.value = data
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        return try {
            gatt.writeCharacteristic(char)
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            // Handled silently
        } finally {
            bluetoothGatt = null
            writeCharacteristic = null
            _state.value = ChannelState.Disconnected
        }
    }
}

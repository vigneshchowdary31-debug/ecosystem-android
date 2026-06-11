package com.ecosystem.core.discovery.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.ecosystem.core.common.BleConstants
import com.ecosystem.core.discovery.domain.model.DiscoveredDevice
import com.ecosystem.core.discovery.domain.repository.DiscoveryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryServiceImpl @Inject constructor(
    private val context: Context
) : DiscoveryService {

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private val bleScanner: android.bluetooth.le.BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val bleAdvertiser: android.bluetooth.le.BluetoothLeAdvertiser?
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private val _scanResults = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val scanResults: StateFlow<List<DiscoveredDevice>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    override val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val discoveredDevicesMap = mutableMapOf<String, DiscoveredDevice>()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address
            val name = result.scanRecord?.deviceName ?: device.name
            val rssi = result.rssi
            val uuids = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()

            val discovered = DiscoveredDevice(
                macAddress = address,
                name = name,
                rssi = rssi,
                serviceUuids = uuids
            )
            discoveredDevicesMap[address] = discovered
            _scanResults.value = discoveredDevicesMap.values.toList().sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            android.util.Log.e("BLE_ADVERTISE_DEBUG", "Scan failed with error code: $errorCode")
            _isScanning.value = false
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            android.util.Log.d("BLE_ADVERTISE_DEBUG", "Advertising started successfully (onStartSuccess)")
            _isAdvertising.value = true
        }

        override fun onStartFailure(errorCode: Int) {
            val reason = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "ADVERTISE_FAILED_ALREADY_STARTED"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "ADVERTISE_FAILED_DATA_TOO_LARGE"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "ADVERTISE_FAILED_INTERNAL_ERROR"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                else -> "UNKNOWN_ERROR ($errorCode)"
            }
            android.util.Log.e("BLE_ADVERTISE_DEBUG", "Advertising failed to start (onStartFailure). Error: $reason")
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScanning(serviceUuid: String?) {
        val hasPermissions = com.ecosystem.core.discovery.BlePermissionHelper.hasPermissions(context)
        val isEnabled = bluetoothAdapter?.isEnabled == true
        val scanner = bleScanner

        android.util.Log.d("BLE_ADVERTISE_DEBUG", "startScanning called: permissions=$hasPermissions, enabled=$isEnabled, scannerAvailable=${scanner != null}")

        if (_isScanning.value || scanner == null || !isEnabled) return

        discoveredDevicesMap.clear()
        _scanResults.value = emptyList()

        val filters = mutableListOf<ScanFilter>()
        val targetUuid = serviceUuid ?: BleConstants.SERVICE_UUID_STRING
        filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(targetUuid))).build())

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(filters.takeIf { it.isNotEmpty() }, settings, scanCallback)
            _isScanning.value = true
        } catch (e: Exception) {
            android.util.Log.e("BLE_ADVERTISE_DEBUG", "Exception starting scan", e)
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScanning() {
        val scanner = bleScanner
        if (!_isScanning.value || scanner == null) return

        try {
            scanner.stopScan(scanCallback)
        } catch (e: Exception) {
            android.util.Log.e("BLE_ADVERTISE_DEBUG", "Exception stopping scan", e)
        } finally {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun startAdvertising(localDeviceId: String, localName: String) {
        val hasPermissions = com.ecosystem.core.discovery.BlePermissionHelper.hasPermissions(context)
        val isSupported = bluetoothAdapter?.isMultipleAdvertisementSupported == true
        val advertiser = bleAdvertiser
        val isEnabled = bluetoothAdapter?.isEnabled == true
        val targetUuid = BleConstants.SERVICE_UUID

        android.util.Log.d("BLE_ADVERTISE_DEBUG", "startAdvertising called:")
        android.util.Log.d("BLE_ADVERTISE_DEBUG", "  - Permissions Granted: $hasPermissions")
        android.util.Log.d("BLE_ADVERTISE_DEBUG", "  - Multiple Adv Supported: $isSupported")
        android.util.Log.d("BLE_ADVERTISE_DEBUG", "  - Advertiser Available: ${advertiser != null}")
        android.util.Log.d("BLE_ADVERTISE_DEBUG", "  - Bluetooth Enabled: $isEnabled")
        android.util.Log.d("BLE_ADVERTISE_DEBUG", "  - Service UUID: $targetUuid")
        android.util.Log.d("BLE_ADVERTISE_DEBUG", "  - Device Name: $localName")

        if (!hasPermissions) {
            android.util.Log.w("BLE_ADVERTISE_DEBUG", "Aborting: Permissions not granted.")
            return
        }
        if (!isSupported) {
            android.util.Log.w("BLE_ADVERTISE_DEBUG", "Aborting: Multiple advertisement not supported by hardware.")
            return
        }
        if (advertiser == null) {
            android.util.Log.w("BLE_ADVERTISE_DEBUG", "Aborting: BluetoothLeAdvertiser is null.")
            return
        }
        if (!isEnabled) {
            android.util.Log.w("BLE_ADVERTISE_DEBUG", "Aborting: Bluetooth is disabled.")
            return
        }
        if (_isAdvertising.value) {
            android.util.Log.d("BLE_ADVERTISE_DEBUG", "Aborting: Already advertising.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        android.util.Log.d("BLE_ADVERTISE_DEBUG", "Advertise Settings: Mode=LowLatency, TxPower=High, Connectable=true")

        // Split data to avoid ADVERTISE_FAILED_DATA_TOO_LARGE
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Do not include in main advertise packet to save space (128-bit UUID is 16 bytes)
            .addServiceUuid(ParcelUuid(targetUuid))
            .build()

        val scanResponseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true) // Include the name in the scan response instead
            .build()

        try {
            android.util.Log.d("BLE_ADVERTISE_DEBUG", "Attempting to start advertising...")
            advertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback)
        } catch (e: Exception) {
            android.util.Log.e("BLE_ADVERTISE_DEBUG", "Exception starting advertising", e)
            _isAdvertising.value = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopAdvertising() {
        val advertiser = bleAdvertiser
        if (!_isAdvertising.value || advertiser == null) return

        try {
            advertiser.stopAdvertising(advertiseCallback)
            android.util.Log.d("BLE_ADVERTISE_DEBUG", "Advertising stopped (stopAdvertising called)")
        } catch (e: Exception) {
            android.util.Log.e("BLE_ADVERTISE_DEBUG", "Exception stopping advertising", e)
        } finally {
            _isAdvertising.value = false
        }
    }
}

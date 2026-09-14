package com.ecosystem.core.networking.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import com.ecosystem.core.common.BleConstants
import com.ecosystem.core.common.BlePermissionHelper
import com.ecosystem.core.common.log.AppLog
import com.ecosystem.core.pairing.ConnectionPhase
import com.ecosystem.core.pairing.PairingCandidate
import com.ecosystem.core.pairing.PairingConnection
import com.ecosystem.core.pairing.PairingTransport
import com.ecosystem.core.pairing.TransportException
import com.ecosystem.core.pairing.TransportReadiness
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE implementation of [PairingTransport]. Scans only for the ConnectFlow service UUID and
 * never uses the device name, address or manufacturer data to decide trust. The address is
 * only a handle for connecting; it may rotate and is not an identity.
 */
@SuppressLint("MissingPermission")
@Singleton
class BlePairingTransport @Inject constructor(
    @ApplicationContext private val context: Context,
) : PairingTransport {

    private val bluetoothManager: BluetoothManager?
        get() = context.getSystemService(BluetoothManager::class.java)

    /** Devices from the most recent scan, keyed by candidate handle. */
    private val lastScanDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val scanStartTimes = ArrayDeque<Long>()

    override fun readiness(): TransportReadiness {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return TransportReadiness.BLUETOOTH_UNAVAILABLE
        }
        val adapter = bluetoothManager?.adapter ?: return TransportReadiness.BLUETOOTH_UNAVAILABLE
        if (!BlePermissionHelper.hasPairingPermissions(context)) return TransportReadiness.PERMISSION_DENIED
        if (!adapter.isEnabled) return TransportReadiness.BLUETOOTH_DISABLED
        // Before Android 12, BLE scan results are only delivered while location services are on.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocationEnabled()) {
            return TransportReadiness.LOCATION_DISABLED
        }
        return TransportReadiness.READY
    }

    override suspend fun scanForCandidates(timeoutMillis: Long, settleMillis: Long): List<PairingCandidate> {
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            throw TransportException(TransportException.Kind.BLUETOOTH_DISABLED, "Bluetooth is off")
        }
        val scanner = adapter.bluetoothLeScanner
            ?: throw TransportException(TransportException.Kind.BLUETOOTH_DISABLED, "BLE scanner unavailable")
        awaitScanSlot()

        val results = Channel<ScanResult>(Channel.UNLIMITED)
        val scanError = AtomicInteger(NO_SCAN_ERROR)
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                results.trySend(result)
            }

            override fun onBatchScanResults(batch: MutableList<ScanResult>) {
                batch.forEach { results.trySend(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                scanError.set(errorCode)
                results.close()
            }
        }
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID)).build())
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            scanner.startScan(filters, settings, callback)
        } catch (e: SecurityException) {
            throw TransportException(TransportException.Kind.PERMISSION_DENIED, "BLUETOOTH_SCAN not granted", cause = e)
        }
        AppLog.d(TAG) { "Scanning for the pairing service" }

        val found = LinkedHashMap<String, ScanResult>()
        try {
            val first = withTimeoutOrNull(timeoutMillis) { results.receiveCatching().getOrNull() }
            val errorCode = scanError.get()
            if (errorCode != NO_SCAN_ERROR) {
                throw TransportException(TransportException.Kind.SCAN_FAILED, "BLE scan failed (error $errorCode)")
            }
            if (first == null) return emptyList()
            found[first.device.address] = first
            withTimeoutOrNull(settleMillis) {
                for (result in results) found[result.device.address] = result
            }
        } finally {
            try {
                scanner.stopScan(callback)
            } catch (ignored: Exception) {
                // Adapter turned off or permission revoked mid-scan; the scan is already gone.
            }
            results.close()
        }

        lastScanDevices.clear()
        found.values.forEach { lastScanDevices[it.device.address] = it.device }
        AppLog.d(TAG) { "Scan found ${found.size} candidate(s)" }
        return found.values
            .sortedByDescending { it.rssi }
            .map { PairingCandidate(handle = it.device.address, rssi = it.rssi) }
    }

    override suspend fun connect(candidate: PairingCandidate, onPhase: (ConnectionPhase) -> Unit): PairingConnection {
        val device = lastScanDevices[candidate.handle]
            ?: throw TransportException(TransportException.Kind.CONNECT_FAILED, "Candidate is not from the latest scan")
        return BleGattConnection.open(context, device, onPhase)
    }

    /** Android throttles apps that start more than 5 scans in 30 seconds; stay under that limit. */
    private suspend fun awaitScanSlot() {
        val waitMillis = synchronized(scanStartTimes) {
            val now = SystemClock.elapsedRealtime()
            while (scanStartTimes.isNotEmpty() && now - scanStartTimes.first() > SCAN_WINDOW_MILLIS) {
                scanStartTimes.removeFirst()
            }
            val wait = if (scanStartTimes.size >= MAX_SCANS_PER_WINDOW) {
                SCAN_WINDOW_MILLIS - (now - scanStartTimes.first()) + 100
            } else 0L
            scanStartTimes.addLast(now + wait)
            wait
        }
        if (waitMillis > 0) {
            AppLog.d(TAG) { "Waiting ${waitMillis}ms to respect the BLE scan limit" }
            delay(waitMillis)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(LocationManager::class.java) ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private companion object {
        const val TAG = "BlePairingTransport"
        const val NO_SCAN_ERROR = -1
        const val MAX_SCANS_PER_WINDOW = 4
        const val SCAN_WINDOW_MILLIS = 30_000L
    }
}

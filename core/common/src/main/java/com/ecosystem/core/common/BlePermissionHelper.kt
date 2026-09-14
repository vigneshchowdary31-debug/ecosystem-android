package com.ecosystem.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BlePermissionHelper {

    /** Runtime permissions needed to scan for and connect to the Mac during pairing. */
    fun pairingPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    /** Everything the BLE debug tools need, including advertising. */
    fun allPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }

    fun hasPairingPermissions(context: Context): Boolean = granted(context, pairingPermissions())

    fun hasAllPermissions(context: Context): Boolean = granted(context, allPermissions())

    /** BLUETOOTH_CONNECT is needed to show the system "turn on Bluetooth" prompt on Android 12+. */
    fun canRequestBluetoothEnable(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            granted(context, listOf(Manifest.permission.BLUETOOTH_CONNECT))

    private fun granted(context: Context, permissions: List<String>): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

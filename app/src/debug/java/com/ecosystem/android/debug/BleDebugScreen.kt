package com.ecosystem.android.debug

import android.bluetooth.BluetoothManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystem.core.common.BlePermissionHelper
import com.ecosystem.core.designsystem.components.GlassCard
import com.ecosystem.core.designsystem.components.GradientButton
import com.ecosystem.core.discovery.domain.model.DiscoveredDevice
import com.ecosystem.core.discovery.domain.repository.DiscoveryService
import com.ecosystem.core.identity.IdentityManager
import kotlinx.coroutines.launch

/**
 * Debug-only BLE console: shows adapter state and devices advertising the ConnectFlow service,
 * and lets a developer advertise this phone. Names and addresses here are for display only.
 */
@Composable
fun BleDebugScreen(identityManager: IdentityManager, discoveryService: DiscoveryService, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adapter = remember { context.getSystemService(BluetoothManager::class.java)?.adapter }
    var hasPermissions by remember { mutableStateOf(BlePermissionHelper.hasAllPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermissions = BlePermissionHelper.hasAllPermissions(context)
    }
    LaunchedEffect(Unit) {
        if (!hasPermissions) permissionLauncher.launch(BlePermissionHelper.allPermissions().toTypedArray())
    }

    val isEnabled = adapter?.isEnabled == true
    val isScanning by discoveryService.isScanning.collectAsState(initial = false)
    val isAdvertising by discoveryService.isAdvertising.collectAsState(initial = false)
    val scanResults by discoveryService.scanResults.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        GlassCard(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("BLE status", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                StatusRow("Bluetooth", if (adapter == null) "Not supported" else if (isEnabled) "On" else "Off", isEnabled)
                StatusRow("Permissions", if (hasPermissions) "Granted" else "Missing", hasPermissions)
                StatusRow("Advertising", if (isAdvertising) "Active" else "Inactive", isAdvertising)
                StatusRow("Scanning", if (isScanning) "Active" else "Inactive", isScanning)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradientButton(
                onClick = {
                    scope.launch {
                        val identity = identityManager.getOrCreateIdentity()
                        discoveryService.startAdvertising(identity.deviceId, identity.name, identity.advertisingIdentifier)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isAdvertising && hasPermissions && isEnabled,
            ) { Text("Advertise", fontSize = 12.sp) }
            GradientButton(onClick = discoveryService::stopAdvertising, modifier = Modifier.weight(1f), enabled = isAdvertising) {
                Text("Stop", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradientButton(
                onClick = { discoveryService.startScanning(null) },
                modifier = Modifier.weight(1f),
                enabled = !isScanning && hasPermissions && isEnabled,
            ) { Text("Scan", fontSize = 12.sp) }
            GradientButton(onClick = discoveryService::stopScanning, modifier = Modifier.weight(1f), enabled = isScanning) {
                Text("Stop", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Nearby devices (${scanResults.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (scanResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(if (isScanning) "Searching…" else "Start a scan to list devices advertising the ConnectFlow service.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scanResults, key = { it.macAddress }) { DeviceRow(it) }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336))
            )
            Spacer(Modifier.width(6.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeviceRow(device: DiscoveredDevice) {
    GlassCard(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(device.name ?: "Unnamed device", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${device.rssi} dBm", fontSize = 14.sp)
            }
            Text(
                "Address (rotates, not an identity): ${device.macAddress}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

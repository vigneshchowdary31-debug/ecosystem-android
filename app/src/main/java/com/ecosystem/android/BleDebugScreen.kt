package com.ecosystem.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystem.core.designsystem.components.GlassCard
import com.ecosystem.core.designsystem.components.GradientButton
import com.ecosystem.core.discovery.BlePermissionHelper
import com.ecosystem.core.discovery.domain.repository.DiscoveryService
import com.ecosystem.core.identity.IdentityManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BleDebugScreen(
    identityManager: IdentityManager,
    discoveryService: DiscoveryService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager?.adapter }
    val isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
    val hasBlePermissions = BlePermissionHelper.hasPermissions(context)
    
    val isScanning by discoveryService.isScanning.collectAsState(initial = false)
    val isAdvertising by discoveryService.isAdvertising.collectAsState(initial = false)
    val scanResults by discoveryService.scanResults.collectAsState(initial = emptyList())
    
    val lastSeenMap = remember { mutableStateMapOf<String, Long>() }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Log BLE status changes
    LaunchedEffect(isScanning) {
        Log.d("BLE_DEBUG", "Scanning state changed: isScanning=$isScanning")
    }
    
    LaunchedEffect(isAdvertising) {
        Log.d("BLE_DEBUG", "Advertising state changed: isAdvertising=$isAdvertising")
    }

    // Keep track of last seen times and log device discovery
    LaunchedEffect(scanResults) {
        val now = System.currentTimeMillis()
        scanResults.forEach { device ->
            val prevTime = lastSeenMap[device.macAddress]
            if (prevTime == null) {
                Log.d("BLE_DEBUG", "Device discovered: Name=${device.name ?: "Unknown"}, RSSI=${device.rssi}, ID=${device.macAddress}")
            }
            lastSeenMap[device.macAddress] = now
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status Section
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "BLE Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                StatusRow(
                    label = "Bluetooth Hardware",
                    value = if (bluetoothAdapter == null) "Not Supported" else if (isBluetoothEnabled) "Enabled" else "Disabled",
                    isActive = isBluetoothEnabled && bluetoothAdapter != null
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val isAdvertisingSupported = bluetoothAdapter?.isMultipleAdvertisementSupported == true
                StatusRow(
                    label = "Advertising Supported",
                    value = if (isAdvertisingSupported) "Yes" else "No",
                    isActive = isAdvertisingSupported
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val isAdvertiserAvailable = bluetoothAdapter?.bluetoothLeAdvertiser != null
                StatusRow(
                    label = "Advertiser Available",
                    value = if (isAdvertiserAvailable) "Yes" else "No",
                    isActive = isAdvertiserAvailable
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusRow(
                    label = "Permission Granted",
                    value = if (hasBlePermissions) "Yes" else "No",
                    isActive = hasBlePermissions
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusRow(
                    label = "Advertising Active",
                    value = if (isAdvertising) "Yes" else "No",
                    isActive = isAdvertising
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusRow(
                    label = "Scanning Status",
                    value = if (isScanning) "Scanning..." else "Inactive",
                    isActive = isScanning
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control Buttons
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "BLE Controls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradientButton(
                        onClick = {
                            scope.launch {
                                Log.d("BLE_DEBUG", "Initiating advertising start")
                                val identity = identityManager.getIdentity() ?: identityManager.getOrCreateIdentity()
                                discoveryService.startAdvertising(identity.deviceId, identity.name, identity.advertisingIdentifier)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isAdvertising && hasBlePermissions && isBluetoothEnabled
                    ) {
                        Text("Start Advertising", fontSize = 12.sp)
                    }
                    
                    GradientButton(
                        onClick = {
                            Log.d("BLE_DEBUG", "Initiating advertising stop")
                            discoveryService.stopAdvertising()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isAdvertising
                    ) {
                        Text("Stop Advertising", fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradientButton(
                        onClick = {
                            Log.d("BLE_DEBUG", "Initiating scan start")
                            discoveryService.startScanning(null)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning && hasBlePermissions && isBluetoothEnabled
                    ) {
                        Text("Start Scan", fontSize = 12.sp)
                    }
                    
                    GradientButton(
                        onClick = {
                            Log.d("BLE_DEBUG", "Initiating scan stop")
                            discoveryService.stopScanning()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isScanning
                    ) {
                        Text("Stop Scan", fontSize = 12.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nearby Devices Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby Devices (${scanResults.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            if (isScanning) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Nearby Devices List
        if (scanResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isScanning) "Searching for devices..." else "Scan inactive. Press 'Start Scan' to discover devices.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scanResults, key = { it.macAddress }) { device ->
                    val lastSeenTime = lastSeenMap[device.macAddress] ?: System.currentTimeMillis()
                    val lastSeenString = timeFormatter.format(Date(lastSeenTime))
                    
                    DeviceListItem(
                        device = device,
                        lastSeen = lastSeenString
                    )
                }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DeviceListItem(device: com.ecosystem.core.discovery.domain.model.DiscoveredDevice, lastSeen: String) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.name ?: "Unnamed Device",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "${device.rssi} dBm",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        device.rssi >= -60 -> Color(0xFF4CAF50)
                        device.rssi >= -80 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${device.macAddress}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                
                Text(
                    text = "Seen: $lastSeen",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

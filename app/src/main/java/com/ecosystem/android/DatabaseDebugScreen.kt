package com.ecosystem.android

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystem.core.designsystem.components.GlassCard
import com.ecosystem.core.designsystem.components.GradientButton
import com.ecosystem.core.trusteddevices.TrustedDeviceEntity
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

@Composable
fun DatabaseDebugScreen(
    repository: TrustedDeviceRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val trustedDevices by repository.getActiveTrustedDevicesFlow().collectAsState(initial = emptyList())
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary & Actions Section
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
                    text = "Room Database Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Number of Trusted Devices",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${trustedDevices.size}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradientButton(
                        onClick = {
                            scope.launch {
                                val randomId = UUID.randomUUID().toString()
                                val testNames = listOf("MacBook Pro", "MacBook Air", "Mac Studio", "Mac mini", "iMac Pro")
                                val randomName = "${testNames.random()} (${Random.nextInt(100, 999)})"
                                val testDevice = TrustedDeviceEntity(
                                    deviceId = randomId,
                                    name = randomName,
                                    bleMacAddress = "AA:BB:CC:DD:EE:${String.format("%02X", Random.nextInt(256))}",
                                    lastKnownIpAddress = "192.168.1.${Random.nextInt(2, 254)}",
                                    publicKeyEd25519 = "mock_pubkey_bytes_" + UUID.randomUUID().toString().take(8),
                                    isTrustActive = true,
                                    pairingTimestamp = System.currentTimeMillis(),
                                    lastSeenTimestamp = System.currentTimeMillis()
                                )
                                repository.addTrustedDevice(testDevice)
                                Log.d("DATABASE_DEBUG", "Inserted test device: ID=${testDevice.deviceId}, Name=${testDevice.name}")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Insert Test Device", fontSize = 12.sp)
                    }
                    
                    GradientButton(
                        onClick = {
                            scope.launch {
                                // Find any device to delete (e.g. the last one)
                                if (trustedDevices.isNotEmpty()) {
                                    val deviceToDelete = trustedDevices.last()
                                    repository.removeTrustedDevice(deviceToDelete)
                                    Log.d("DATABASE_DEBUG", "Deleted test device: ID=${deviceToDelete.deviceId}")
                                } else {
                                    Log.d("DATABASE_DEBUG", "No devices available to delete")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = trustedDevices.isNotEmpty()
                    ) {
                        Text("Delete Test Device", fontSize = 12.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Trusted Devices Header
        Text(
            text = "Trusted Devices List",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Trusted Devices Scrollable List
        if (trustedDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trusted devices stored in database.",
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
                items(trustedDevices, key = { it.deviceId }) { device ->
                    val formattedPairingDate = dateFormatter.format(Date(device.pairingTimestamp))
                    
                    DeviceDbItem(
                        device = device,
                        pairingDate = formattedPairingDate,
                        onDeleteClick = {
                            scope.launch {
                                repository.removeTrustedDevice(device)
                                Log.d("DATABASE_DEBUG", "Deleted device: ID=${device.deviceId}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceDbItem(
    device: TrustedDeviceEntity,
    pairingDate: String,
    onDeleteClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Trust Status Indicator
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (device.isTrustActive) Color(0xFF4CAF50) else Color(0xFFF44336))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (device.isTrustActive) "Trusted" else "Revoked",
                        fontSize = 11.sp,
                        color = if (device.isTrustActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "ID: ${device.deviceId}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Paired on: $pairingDate",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Device",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

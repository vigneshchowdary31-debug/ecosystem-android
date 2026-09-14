package com.ecosystem.android.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystem.core.designsystem.components.GlassCard
import com.ecosystem.core.designsystem.components.GradientButton
import com.ecosystem.core.trusteddevices.TrustedDevice
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Debug-only view of the trusted-device store. Test devices use random but well-formed keys. */
@Composable
fun DatabaseDebugScreen(repository: TrustedDeviceRepository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val devices by repository.observeTrustedDevices().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Trusted devices (${devices.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        GradientButton(
            onClick = {
                scope.launch {
                    val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
                    repository.savePairedDevice(UUID.randomUUID().toString(), "Test Mac", key, "192.168.1.99", System.currentTimeMillis())
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Insert test device") }
        Spacer(Modifier.height(16.dp))
        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No trusted devices.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.deviceId }) { device ->
                    TrustedDeviceRow(
                        device = device,
                        pairedAt = dateFormat.format(Date(device.pairedAt)),
                        lastSeen = dateFormat.format(Date(device.lastSeenAt)),
                        onRevoke = { scope.launch { repository.revoke(device.deviceId) } },
                        onDelete = { scope.launch { repository.delete(device.deviceId) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustedDeviceRow(
    device: TrustedDevice,
    pairedAt: String,
    lastSeen: String,
    onRevoke: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(device.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("ID: ${device.deviceId}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text("Paired: $pairedAt · Last seen: $lastSeen", fontSize = 12.sp)
            Text("Address hint: ${device.lastKnownIpAddress ?: "none"}", fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onRevoke) { Text("Revoke") }
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

package com.ecosystem.android

import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystem.core.designsystem.components.GlassCard
import com.ecosystem.core.designsystem.components.GradientButton
import com.ecosystem.core.identity.DeviceInfo
import com.ecosystem.core.identity.IdentityManager
import kotlinx.coroutines.launch

@Composable
fun IdentityDebugScreen(
    identityManager: IdentityManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "1.0.0-debug"
        }
    }

    val platform = "Android (API ${android.os.Build.VERSION.SDK_INT})"

    fun loadIdentity() {
        isLoading = true
        scope.launch {
            val identity = identityManager.getOrCreateIdentity()
            deviceInfo = identity
            isLoading = false
            Log.d("IDENTITY_DEBUG", identity.deviceId)
            Log.d("SECURITY_DEBUG", identity.publicKeyEd25519)
        }
    }

    fun reloadKey() {
        isLoading = true
        scope.launch {
            val identity = identityManager.getIdentity() ?: identityManager.getOrCreateIdentity()
            deviceInfo = identity
            isLoading = false
            Log.d("SECURITY_DEBUG", identity.publicKeyEd25519)
        }
    }

    LaunchedEffect(Unit) {
        loadIdentity()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sprint 1 Debug Validation",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    deviceInfo?.let { info ->
                        DebugItem(label = "Device Name", value = info.name)
                        Spacer(modifier = Modifier.height(12.dp))
                        DebugItem(label = "Device UUID", value = info.deviceId)
                        Spacer(modifier = Modifier.height(12.dp))
                        DebugItem(label = "BLE Advertising ID", value = info.advertisingIdentifier)
                        Spacer(modifier = Modifier.height(12.dp))
                        DebugItem(label = "App Version", value = appVersion)
                        Spacer(modifier = Modifier.height(12.dp))
                        DebugItem(label = "Platform", value = platform)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Decode Base64 key length for display
                        val rawBytes = remember(info.publicKeyEd25519) {
                            try {
                                Base64.decode(info.publicKeyEd25519, Base64.DEFAULT)
                            } catch (e: Exception) {
                                byteArrayOf()
                            }
                        }
                        val keyLength = "${rawBytes.size * 8} bits (${rawBytes.size} bytes)"
                        
                        DebugItem(label = "Ed25519 Public Key", value = info.publicKeyEd25519)
                        Spacer(modifier = Modifier.height(12.dp))
                        DebugItem(label = "Key Length", value = keyLength)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val creationDate = remember(info.createdAt) {
                            if (info.createdAt > 0L) {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(info.createdAt))
                            } else {
                                "Unknown"
                            }
                        }
                        DebugItem(label = "Key Creation Date", value = creationDate)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                GradientButton(
                    onClick = { loadIdentity() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh Identity")
                }

                Spacer(modifier = Modifier.height(8.dp))

                GradientButton(
                    onClick = { reloadKey() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reload Key")
                }
            }
        }
    }
}

@Composable
fun DebugItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

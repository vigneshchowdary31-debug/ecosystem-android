package com.ecosystem.android.debug

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
import androidx.compose.material3.TextButton
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
import com.ecosystem.core.security.IdentityKeyUnavailableException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

/** Debug-only: shows the device identity and allows an explicit identity reset. */
@Composable
fun IdentityDebugScreen(identityManager: IdentityManager, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun load(block: suspend () -> DeviceInfo) {
        isLoading = true
        scope.launch {
            try {
                deviceInfo = block()
                error = null
            } catch (e: IdentityKeyUnavailableException) {
                deviceInfo = null
                error = "Identity material can't be decrypted (Keystore key lost). Reset the identity to continue."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load { identityManager.getOrCreateIdentity() } }

    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassCard(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Device identity", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
                when {
                    isLoading -> CircularProgressIndicator()
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    else -> deviceInfo?.let { info ->
                        val keyBytes = remember(info.publicKeyEd25519) { Base64.getDecoder().decode(info.publicKeyEd25519).size }
                        DebugItem("Device name", info.name)
                        DebugItem("Device ID", info.deviceId)
                        DebugItem("Advertising ID", info.advertisingIdentifier)
                        DebugItem("Ed25519 public key", info.publicKeyEd25519)
                        DebugItem("Key length", "$keyBytes bytes")
                        DebugItem("Key protection", "Software Ed25519 key, sealed at rest with an Android Keystore AES-256-GCM key")
                        DebugItem(
                            "Created",
                            if (info.createdAt > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(info.createdAt)) else "Unknown"
                        )
                        DebugItem("App version", appVersion)
                        DebugItem("Platform", "Android (API ${android.os.Build.VERSION.SDK_INT})")
                    }
                }
                Spacer(Modifier.height(24.dp))
                GradientButton(onClick = { load { identityManager.getOrCreateIdentity() } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reload identity")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { load { identityManager.resetIdentity() } }) {
                    Text("Reset identity (Macs must pair again)", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun DebugItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

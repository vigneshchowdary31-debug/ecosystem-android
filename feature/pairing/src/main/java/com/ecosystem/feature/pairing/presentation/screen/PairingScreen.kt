package com.ecosystem.feature.pairing.presentation.screen

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ecosystem.core.common.BlePermissionHelper
import com.ecosystem.core.designsystem.components.GlassCard
import com.ecosystem.core.designsystem.components.GradientButton
import com.ecosystem.feature.pairing.presentation.PairingAction
import com.ecosystem.feature.pairing.presentation.PairingOutcome
import com.ecosystem.feature.pairing.presentation.PairingUiState
import com.ecosystem.feature.pairing.presentation.PairingViewModel

@Composable
fun PairingScreen(
    viewModel: PairingViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ui by viewModel.uiState.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    var sendToPermissionSettings by rememberSaveable { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setCameraPermissionGranted(granted) }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.isNotEmpty() && results.values.all { it }) {
            sendToPermissionSettings = false
            if (ui.outcome == PairingOutcome.FAILURE) viewModel.retry()
        } else {
            // After a denial Android may stop showing the dialog; the next tap opens app settings instead.
            sendToPermissionSettings = true
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.retry() }

    LaunchedEffect(Unit) {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.setCameraPermissionGranted(cameraGranted)
        if (!cameraGranted) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun perform(action: PairingAction) {
        when (action) {
            PairingAction.RETRY -> viewModel.retry()
            PairingAction.SCAN_ANOTHER, PairingAction.PAIR_ANOTHER -> viewModel.scanAnotherCode()
            PairingAction.CANCEL -> viewModel.cancel()
            PairingAction.ENABLE_BLUETOOTH ->
                if (BlePermissionHelper.canRequestBluetoothEnable(context)) {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    blePermissionLauncher.launch(BlePermissionHelper.pairingPermissions().toTypedArray())
                }
            PairingAction.GRANT_PERMISSION ->
                if (sendToPermissionSettings) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    blePermissionLauncher.launch(BlePermissionHelper.pairingPermissions().toTypedArray())
                }
            PairingAction.OPEN_LOCATION_SETTINGS ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            ui.showScanner && !hasCameraPermission -> CameraPermissionPrompt(
                onGrant = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            )
            ui.showScanner -> ScannerView(ui = ui, onQrDecoded = viewModel::onQrCodeScanned)
            else -> StatusCard(ui = ui, onAction = ::perform)
        }
    }
}

@Composable
private fun ScannerView(ui: PairingUiState, onQrDecoded: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        QrScanner(onQrDecoded = onQrDecoded, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.size(260.dp)) {
            GlassCard(modifier = Modifier.fillMaxSize(), borderWidth = 2.dp, cornerRadius = 24.dp) {}
        }
        GlassCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(ui.title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                ui.detail?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(ui: PairingUiState, onAction: (PairingAction) -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .wrapContentHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            when (ui.outcome) {
                PairingOutcome.SUCCESS -> OutcomeBadge("✓", MaterialTheme.colorScheme.tertiary)
                PairingOutcome.FAILURE -> OutcomeBadge("!", MaterialTheme.colorScheme.error)
                PairingOutcome.CANCELLED -> OutcomeBadge("×", MaterialTheme.colorScheme.onSurfaceVariant)
                PairingOutcome.NONE -> if (ui.inProgress) CircularProgressIndicator()
            }
            Spacer(Modifier.height(16.dp))
            Text(ui.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
            ui.detail?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ui.step?.let { step ->
                Spacer(Modifier.height(16.dp))
                StepIndicator(step = step, total = PairingUiState.TOTAL_STEPS)
            }
            Spacer(Modifier.height(24.dp))
            ui.primaryAction?.let { action ->
                if (action == PairingAction.CANCEL) {
                    Button(onClick = { onAction(action) }) { Text(actionLabel(action)) }
                } else {
                    GradientButton(onClick = { onAction(action) }) { Text(actionLabel(action)) }
                }
            }
            ui.secondaryAction?.let { action ->
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onAction(action) }) { Text(actionLabel(action)) }
            }
        }
    }
}

@Composable
private fun OutcomeBadge(symbol: String, color: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepIndicator(step: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Step $step of $total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CameraPermissionPrompt(onGrant: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("Camera access needed", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "ConnectFlow uses the camera only to scan the pairing code shown on your Mac.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGrant) { Text("Allow camera") }
    }
}

private fun actionLabel(action: PairingAction): String = when (action) {
    PairingAction.RETRY -> "Try again"
    PairingAction.SCAN_ANOTHER -> "Scan another code"
    PairingAction.PAIR_ANOTHER -> "Pair another Mac"
    PairingAction.CANCEL -> "Cancel"
    PairingAction.ENABLE_BLUETOOTH -> "Turn on Bluetooth"
    PairingAction.GRANT_PERMISSION -> "Allow Nearby devices"
    PairingAction.OPEN_LOCATION_SETTINGS -> "Open Location settings"
}

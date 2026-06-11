package com.ecosystem.feature.pairing.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ecosystem.core.designsystem.components.GlassCard
import com.ecosystem.core.designsystem.components.GradientButton
import com.ecosystem.core.pairing.PairingState
import com.ecosystem.feature.pairing.presentation.PairingViewModel
import com.ecosystem.feature.pairing.presentation.QrCodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun PairingScreen(
    viewModel: PairingViewModel,
    onPairingSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pairingState by viewModel.pairingState.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.setCameraPermissionGranted(granted)
        }
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setCameraPermissionGranted(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(pairingState) {
        if (pairingState is PairingState.Success) {
            onPairingSuccess()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission && pairingState is PairingState.Idle) {
            // Camera scanner preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(executor, QrCodeAnalyzer { qrText ->
                                    viewModel.onQrCodeScanned(qrText)
                                })
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            // Bind failed
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning reticle guide
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.Center)
            ) {
                // Draw a simple card layout for scanner reticle
                GlassCard(
                    modifier = Modifier.fillMaxSize(),
                    borderWidth = 2.dp,
                    cornerRadius = 24.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Align Pairing QR Code",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (!hasCameraPermission) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This application requires Camera access to scan the pairing QR code displayed on your macOS companion app.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }

        // Overlaid State Status Card
        AnimatedVisibility(
            visible = pairingState !is PairingState.Idle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        when (val state = pairingState) {
                            is PairingState.ParsingQr -> {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Verifying QR Code...", fontWeight = FontWeight.SemiBold)
                            }
                            is PairingState.ConnectingBle -> {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Connecting to macOS via BLE...", fontWeight = FontWeight.SemiBold)
                            }
                            is PairingState.EphemeralKeyAgreement -> {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Performing Key Agreement...", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.SpacerHeight(4))
                                Text("Exchanging X25519 public keys...", fontSize = 12.sp, color = Color.Gray)
                            }
                            is PairingState.SignatureVerification -> {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Authenticating Signatures...", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.SpacerHeight(4))
                                Text("Verifying Ed25519 challenge...", fontSize = 12.sp, color = Color.Gray)
                            }
                            is PairingState.Success -> {
                                Text("🎉", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Pairing Successful!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Green)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("This device is now trusted.", textAlign = TextAlign.Center)
                            }
                            is PairingState.Failed -> {
                                Text("❌", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Pairing Failed", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.reason, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(24.dp))
                                GradientButton(onClick = { viewModel.onRetry() }) {
                                    Text("Retry Scan")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

// Utility spacer extension
@Composable
private fun Modifier.SpacerHeight(height: Int): Modifier = this.height(height.dp)

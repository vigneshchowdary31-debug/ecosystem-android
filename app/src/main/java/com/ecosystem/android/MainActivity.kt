package com.ecosystem.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecosystem.core.designsystem.theme.CompanionAppTheme
import com.ecosystem.core.discovery.BlePermissionHelper
import com.ecosystem.core.discovery.domain.repository.DiscoveryService
import com.ecosystem.core.identity.IdentityManager
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository
import com.ecosystem.feature.pairing.presentation.PairingViewModel
import com.ecosystem.feature.pairing.presentation.screen.PairingScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var identityManager: IdentityManager

    @Inject
    lateinit var discoveryService: DiscoveryService

    @Inject
    lateinit var trustedDeviceRepository: TrustedDeviceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            CompanionAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { }

                    LaunchedEffect(Unit) {
                        if (!BlePermissionHelper.hasPermissions(this@MainActivity)) {
                            permissionLauncher.launch(
                                BlePermissionHelper.getRequiredPermissions().toTypedArray()
                            )
                        }
                    }

                    var selectedTab by remember { mutableStateOf(0) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Identity Debug") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("BLE Debug") }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Database Debug") }
                            )
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = { Text("Pair Device") }
                            )
                        }

                        when (selectedTab) {
                            0 -> IdentityDebugScreen(identityManager = identityManager)
                            1 -> BleDebugScreen(
                                identityManager = identityManager,
                                discoveryService = discoveryService
                            )
                            2 -> DatabaseDebugScreen(repository = trustedDeviceRepository)
                            3 -> {
                                val pairingViewModel: PairingViewModel = hiltViewModel()
                                PairingScreen(
                                    viewModel = pairingViewModel,
                                    onPairingSuccess = { selectedTab = 2 }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


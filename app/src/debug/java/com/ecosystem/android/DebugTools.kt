package com.ecosystem.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ecosystem.android.debug.BleDebugScreen
import com.ecosystem.android.debug.DatabaseDebugScreen
import com.ecosystem.android.debug.IdentityDebugScreen
import com.ecosystem.core.discovery.domain.repository.DiscoveryService
import com.ecosystem.core.identity.IdentityManager
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugToolsEntryPoint {
    fun identityManager(): IdentityManager
    fun discoveryService(): DiscoveryService
    fun trustedDeviceRepository(): TrustedDeviceRepository
}

/** Developer tools. This file exists only in the debug source set, so release builds cannot reach them. */
object DebugTools {
    @Composable
    fun tabs(): List<DebugTab> {
        val context = LocalContext.current
        val entryPoint = remember(context) {
            EntryPointAccessors.fromApplication(context.applicationContext, DebugToolsEntryPoint::class.java)
        }
        return remember(entryPoint) {
            listOf(
                DebugTab("Identity") { IdentityDebugScreen(entryPoint.identityManager()) },
                DebugTab("BLE") { BleDebugScreen(entryPoint.identityManager(), entryPoint.discoveryService()) },
                DebugTab("Trusted devices") { DatabaseDebugScreen(entryPoint.trustedDeviceRepository()) },
            )
        }
    }
}

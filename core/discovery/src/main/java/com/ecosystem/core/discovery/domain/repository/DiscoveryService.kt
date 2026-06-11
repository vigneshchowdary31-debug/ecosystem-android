package com.ecosystem.core.discovery.domain.repository

import com.ecosystem.core.discovery.domain.model.DiscoveredDevice
import kotlinx.coroutines.flow.Flow

interface DiscoveryService {
    val scanResults: Flow<List<DiscoveredDevice>>
    val isScanning: Flow<Boolean>
    val isAdvertising: Flow<Boolean>

    fun startScanning(serviceUuid: String? = null)
    fun stopScanning()
    
    fun startAdvertising(localDeviceId: String, localName: String)
    fun stopAdvertising()
}

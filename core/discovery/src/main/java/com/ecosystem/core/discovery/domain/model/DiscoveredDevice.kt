package com.ecosystem.core.discovery.domain.model

data class DiscoveredDevice(
    val macAddress: String,
    val name: String?,
    val rssi: Int,
    val serviceUuids: List<String> = emptyList(),
    val advertisingIdentifier: String? = null
)

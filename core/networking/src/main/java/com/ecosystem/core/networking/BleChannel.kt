package com.ecosystem.core.networking

import kotlinx.coroutines.flow.Flow

interface BleChannel {
    val incomingPackets: Flow<ByteArray>
    val state: Flow<ChannelState>
    
    suspend fun connect(macAddress: String): Boolean
    suspend fun send(data: ByteArray): Boolean
    fun disconnect()
}

sealed class ChannelState {
    object Disconnected : ChannelState()
    object Connecting : ChannelState()
    object Connected : ChannelState()
    data class Error(val message: String) : ChannelState()
}

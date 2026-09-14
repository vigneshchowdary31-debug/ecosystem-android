package com.ecosystem.core.networking.di

import com.ecosystem.core.networking.ble.BlePairingTransport
import com.ecosystem.core.pairing.PairingTransport
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkingModule {

    @Binds
    @Singleton
    abstract fun bindPairingTransport(impl: BlePairingTransport): PairingTransport
}

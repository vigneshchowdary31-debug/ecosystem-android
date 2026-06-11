package com.ecosystem.core.pairing.di

import com.ecosystem.core.identity.IdentityManager
import com.ecosystem.core.networking.BleChannel
import com.ecosystem.core.security.CryptoManager
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository
import com.ecosystem.core.pairing.PairingManager
import com.ecosystem.core.pairing.PairingManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PairingModule {

    @Binds
    @Singleton
    abstract fun bindPairingManager(
        pairingManagerImpl: PairingManagerImpl
    ): PairingManager
}

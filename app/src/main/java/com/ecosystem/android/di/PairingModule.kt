package com.ecosystem.android.di

import com.ecosystem.android.pairing.AndroidPairingLogger
import com.ecosystem.android.pairing.IdentityPairingSource
import com.ecosystem.android.pairing.RoomTrustedPeerStore
import com.ecosystem.android.pairing.WifiAddressProvider
import com.ecosystem.core.pairing.PairingCoordinator
import com.ecosystem.core.pairing.PairingManager
import com.ecosystem.core.pairing.PairingTransport
import com.ecosystem.core.protocol.crypto.HandshakeCrypto
import com.ecosystem.core.protocol.crypto.TinkHandshakeCrypto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import java.security.SecureRandom
import javax.inject.Singleton

/**
 * Composition root for pairing. The pairing domain and protocol are plain Kotlin with no DI
 * annotations, so they are assembled here from the Android implementations of their ports.
 */
@Module
@InstallIn(SingletonComponent::class)
object PairingModule {

    /** All handshake randomness (ephemeral keys, nonces) comes from the platform CSPRNG. */
    @Provides
    @Singleton
    fun provideHandshakeCrypto(): HandshakeCrypto = TinkHandshakeCrypto(SecureRandom())

    @Provides
    @Singleton
    fun providePairingManager(
        @ApplicationScope scope: CoroutineScope,
        transport: PairingTransport,
        identitySource: IdentityPairingSource,
        peerStore: RoomTrustedPeerStore,
        crypto: HandshakeCrypto,
        addressProvider: WifiAddressProvider,
    ): PairingManager = PairingCoordinator(
        scope = scope,
        transport = transport,
        identitySource = identitySource,
        peerStore = peerStore,
        crypto = crypto,
        addressProvider = addressProvider,
        logger = AndroidPairingLogger,
    )
}

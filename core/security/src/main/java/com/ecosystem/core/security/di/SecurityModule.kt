package com.ecosystem.core.security.di

import android.content.Context
import com.ecosystem.core.security.KeystoreManager
import com.ecosystem.core.security.SecureStorage
import com.ecosystem.core.security.SecureStorageImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager {
        return KeystoreManager()
    }

    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): SecureStorage {
        return SecureStorageImpl(context, keystoreManager)
    }

    @Provides
    @Singleton
    fun provideCryptoManager(
        secureStorage: SecureStorage
    ): com.ecosystem.core.security.CryptoManager {
        return com.ecosystem.core.security.CryptoManagerImpl(secureStorage)
    }
}

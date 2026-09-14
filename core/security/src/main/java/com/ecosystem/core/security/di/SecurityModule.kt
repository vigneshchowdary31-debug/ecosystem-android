package com.ecosystem.core.security.di

import com.ecosystem.core.security.CryptoManager
import com.ecosystem.core.security.CryptoManagerImpl
import com.ecosystem.core.security.SecureStorage
import com.ecosystem.core.security.SecureStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Implementations use constructor injection; this module only binds interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindSecureStorage(impl: SecureStorageImpl): SecureStorage

    @Binds
    @Singleton
    abstract fun bindCryptoManager(impl: CryptoManagerImpl): CryptoManager
}

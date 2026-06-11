package com.ecosystem.core.identity.di

import com.ecosystem.core.identity.IdentityManager
import com.ecosystem.core.identity.IdentityManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IdentityModule {

    @Binds
    @Singleton
    abstract fun bindIdentityManager(
        identityManagerImpl: IdentityManagerImpl
    ): IdentityManager
}

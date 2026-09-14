package com.ecosystem.core.discovery.di

import com.ecosystem.core.discovery.data.ble.DiscoveryServiceImpl
import com.ecosystem.core.discovery.domain.repository.DiscoveryService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryModule {

    @Binds
    @Singleton
    abstract fun bindDiscoveryService(impl: DiscoveryServiceImpl): DiscoveryService
}

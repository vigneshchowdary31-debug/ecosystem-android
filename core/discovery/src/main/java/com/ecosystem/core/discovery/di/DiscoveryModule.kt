package com.ecosystem.core.discovery.di

import android.content.Context
import com.ecosystem.core.discovery.data.ble.DiscoveryServiceImpl
import com.ecosystem.core.discovery.domain.repository.DiscoveryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiscoveryModule {

    @Provides
    @Singleton
    fun provideDiscoveryService(
        @ApplicationContext context: Context
    ): DiscoveryService {
        return DiscoveryServiceImpl(context)
    }
}

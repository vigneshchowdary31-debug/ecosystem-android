package com.ecosystem.core.networking.di

import android.content.Context
import com.ecosystem.core.networking.BleChannel
import com.ecosystem.core.networking.BleChannelImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {

    @Provides
    @Singleton
    fun provideBleChannel(
        @ApplicationContext context: Context
    ): BleChannel {
        return BleChannelImpl(context)
    }
}

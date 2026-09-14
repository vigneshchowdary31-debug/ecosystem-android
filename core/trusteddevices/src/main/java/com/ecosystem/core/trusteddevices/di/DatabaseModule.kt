package com.ecosystem.core.trusteddevices.di

import android.content.Context
import androidx.room.Room
import com.ecosystem.core.identity.LocalIdentityDao
import com.ecosystem.core.trusteddevices.AppDatabase
import com.ecosystem.core.trusteddevices.AppDatabaseMigrations
import com.ecosystem.core.trusteddevices.TrustedDeviceDao
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository
import com.ecosystem.core.trusteddevices.TrustedDeviceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindTrustedDeviceRepository(impl: TrustedDeviceRepositoryImpl): TrustedDeviceRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
                // No destructive fallback: a missing migration must fail in testing, never erase trusted devices.
                .addMigrations(*AppDatabaseMigrations.ALL)
                .build()

        @Provides
        @Singleton
        fun provideTrustedDeviceDao(database: AppDatabase): TrustedDeviceDao = database.trustedDeviceDao()

        @Provides
        @Singleton
        fun provideLocalIdentityDao(database: AppDatabase): LocalIdentityDao = database.localIdentityDao()
    }
}

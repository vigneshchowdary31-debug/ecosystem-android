package com.ecosystem.core.trusteddevices.di

import android.content.Context
import androidx.room.Room
import com.ecosystem.core.identity.LocalIdentityDao
import com.ecosystem.core.trusteddevices.AppDatabase
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
    abstract fun bindTrustedDeviceRepository(
        impl: TrustedDeviceRepositoryImpl
    ): TrustedDeviceRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "continuity_trusted_devices_db"
            )
            .fallbackToDestructiveMigration()
            .build()
        }

        @Provides
        @Singleton
        fun provideTrustedDeviceDao(database: AppDatabase): TrustedDeviceDao {
            return database.trustedDeviceDao()
        }

        @Provides
        @Singleton
        fun provideLocalIdentityDao(database: AppDatabase): LocalIdentityDao {
            return database.localIdentityDao()
        }
    }
}

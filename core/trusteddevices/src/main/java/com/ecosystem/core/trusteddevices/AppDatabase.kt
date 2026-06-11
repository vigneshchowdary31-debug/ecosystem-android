package com.ecosystem.core.trusteddevices

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ecosystem.core.identity.LocalIdentityDao
import com.ecosystem.core.identity.LocalIdentityEntity

@Database(
    entities = [
        TrustedDeviceEntity::class,
        LocalIdentityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trustedDeviceDao(): TrustedDeviceDao
    abstract fun localIdentityDao(): LocalIdentityDao
}

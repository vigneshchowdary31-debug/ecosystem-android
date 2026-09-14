package com.ecosystem.core.trusteddevices

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ecosystem.core.identity.LocalIdentityDao
import com.ecosystem.core.identity.LocalIdentityEntity

/**
 * Schema history (exported JSON in `core/trusteddevices/schemas/`):
 * - v1: trusted_devices + local_identity
 * - v2: local_identity.advertisingIdentifier added
 * - v3: trusted_devices.bleMacAddress dropped; device IDs canonicalised to lower case
 *
 * Every step has a migration in [AppDatabaseMigrations]. There is no destructive fallback.
 */
@Database(
    entities = [
        TrustedDeviceEntity::class,
        LocalIdentityEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trustedDeviceDao(): TrustedDeviceDao
    abstract fun localIdentityDao(): LocalIdentityDao

    companion object {
        const val NAME = "continuity_trusted_devices_db"
    }
}

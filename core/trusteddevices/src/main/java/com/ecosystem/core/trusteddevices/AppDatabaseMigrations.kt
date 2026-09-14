package com.ecosystem.core.trusteddevices

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {

    /**
     * v1 → v2: `local_identity` gained a NOT NULL `advertisingIdentifier`. SQLite cannot add a
     * NOT NULL column without a default that Room would then reject, so the table is rebuilt.
     * The row is only a cache (identity values live in SecureStorage), so '' is a safe filler.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `local_identity_new` (`idAlias` TEXT NOT NULL, `deviceId` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, `publicKeyEd25519` TEXT NOT NULL, `advertisingIdentifier` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`idAlias`))"
            )
            db.execSQL(
                "INSERT INTO `local_identity_new` (`idAlias`, `deviceId`, `name`, `publicKeyEd25519`, `advertisingIdentifier`, `createdAt`) " +
                    "SELECT `idAlias`, `deviceId`, `name`, `publicKeyEd25519`, '', `createdAt` FROM `local_identity`"
            )
            db.execSQL("DROP TABLE `local_identity`")
            db.execSQL("ALTER TABLE `local_identity_new` RENAME TO `local_identity`")
        }
    }

    /**
     * v2 → v3: drops `bleMacAddress` (rotating Mac addresses must never identify a device) and
     * stores device IDs in canonical lower case. Every trusted device is kept.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `trusted_devices_new` (`deviceId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`lastKnownIpAddress` TEXT, `publicKeyEd25519` TEXT NOT NULL, `isTrustActive` INTEGER NOT NULL, " +
                    "`pairingTimestamp` INTEGER NOT NULL, `lastSeenTimestamp` INTEGER NOT NULL, PRIMARY KEY(`deviceId`))"
            )
            // Oldest pairing first: if two IDs differ only in case, the most recent pairing wins.
            db.execSQL(
                "INSERT OR REPLACE INTO `trusted_devices_new` (`deviceId`, `name`, `lastKnownIpAddress`, `publicKeyEd25519`, " +
                    "`isTrustActive`, `pairingTimestamp`, `lastSeenTimestamp`) " +
                    "SELECT lower(`deviceId`), `name`, `lastKnownIpAddress`, `publicKeyEd25519`, `isTrustActive`, " +
                    "`pairingTimestamp`, `lastSeenTimestamp` FROM `trusted_devices` ORDER BY `pairingTimestamp` ASC"
            )
            db.execSQL("DROP TABLE `trusted_devices`")
            db.execSQL("ALTER TABLE `trusted_devices_new` RENAME TO `trusted_devices`")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}

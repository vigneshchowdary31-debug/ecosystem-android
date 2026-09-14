package com.ecosystem.core.trusteddevices

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Upgrades real SQLite databases built from the exported v1/v2 schemas and checks nothing is lost. */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val dbName = "migration-test.db"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val macKey = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
    private val otherKey = "AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI="

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun v2ToV3KeepsEveryTrustedDevice() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO trusted_devices VALUES ('2D861D8A-6B83-4A1D-8F9C-76E48C3F8F11', 'Studio Mac', " +
                    "'AA:BB:CC:DD:EE:FF', '192.168.1.10', '$macKey', 1, 1000, 2000)"
            )
            db.execSQL(
                "INSERT INTO trusted_devices VALUES ('a0000000-0000-4000-8000-000000000001', 'Old Mac', " +
                    "'11:22:33:44:55:66', NULL, '$otherKey', 0, 500, 600)"
            )
            db.execSQL("INSERT INTO local_identity VALUES ('primary_device_identity', 'id', 'Pixel', 'pk', 'adv', 42)")
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, *AppDatabaseMigrations.ALL)

        db.query(
            "SELECT deviceId, name, lastKnownIpAddress, publicKeyEd25519, isTrustActive, pairingTimestamp, " +
                "lastSeenTimestamp FROM trusted_devices ORDER BY pairingTimestamp"
        ).use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals("a0000000-0000-4000-8000-000000000001", cursor.getString(0))
            assertEquals(0, cursor.getInt(4))
            assertNull(cursor.getString(2))
            cursor.moveToNext()
            assertEquals("2d861d8a-6b83-4a1d-8f9c-76e48c3f8f11", cursor.getString(0))
            assertEquals("Studio Mac", cursor.getString(1))
            assertEquals("192.168.1.10", cursor.getString(2))
            assertEquals(macKey, cursor.getString(3))
            assertEquals(1, cursor.getInt(4))
            assertEquals(1000L, cursor.getLong(5))
            assertEquals(2000L, cursor.getLong(6))
        }
        val columns = db.query("PRAGMA table_info(trusted_devices)").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
        }
        assertFalse("bleMacAddress" in columns)
        db.query("SELECT advertisingIdentifier FROM local_identity").use { cursor ->
            cursor.moveToFirst()
            assertEquals("adv", cursor.getString(0))
        }
    }

    @Test
    fun v1ToV3KeepsTrustedDevicesAndTheIdentityRow() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO trusted_devices VALUES ('2d861d8a-6b83-4a1d-8f9c-76e48c3f8f11', 'Studio Mac', " +
                    "'AA:BB:CC:DD:EE:FF', NULL, '$macKey', 1, 1000, 2000)"
            )
            db.execSQL("INSERT INTO local_identity VALUES ('primary_device_identity', 'id', 'Pixel', 'pk', 42)")
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, *AppDatabaseMigrations.ALL)

        db.query("SELECT deviceId, publicKeyEd25519 FROM trusted_devices").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(macKey, cursor.getString(1))
        }
        db.query("SELECT name, advertisingIdentifier, createdAt FROM local_identity").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Pixel", cursor.getString(0))
            assertEquals("", cursor.getString(1))
            assertEquals(42L, cursor.getLong(2))
        }
    }

    @Test
    fun roomOpensTheMigratedDatabaseAndTheRepositoryReadsIt() = runTest {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO trusted_devices VALUES ('2D861D8A-6B83-4A1D-8F9C-76E48C3F8F11', 'Studio Mac', " +
                    "'AA:BB:CC:DD:EE:FF', '192.168.1.10', '$macKey', 1, 1000, 2000)"
            )
        }
        helper.runMigrationsAndValidate(dbName, 3, true, *AppDatabaseMigrations.ALL).close()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()
        try {
            val devices = TrustedDeviceRepositoryImpl(database.trustedDeviceDao()).getTrustedDevices()
            assertEquals(1, devices.size)
            assertEquals("2d861d8a-6b83-4a1d-8f9c-76e48c3f8f11", devices.single().deviceId)
            assertEquals(1000L, devices.single().pairedAt)
        } finally {
            database.close()
        }
    }
}

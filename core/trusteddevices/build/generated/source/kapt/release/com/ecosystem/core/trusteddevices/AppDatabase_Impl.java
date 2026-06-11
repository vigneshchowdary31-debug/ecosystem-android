package com.ecosystem.core.trusteddevices;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.ecosystem.core.identity.LocalIdentityDao;
import com.ecosystem.core.identity.LocalIdentityDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile TrustedDeviceDao _trustedDeviceDao;

  private volatile LocalIdentityDao _localIdentityDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `trusted_devices` (`deviceId` TEXT NOT NULL, `name` TEXT NOT NULL, `bleMacAddress` TEXT NOT NULL, `lastKnownIpAddress` TEXT, `publicKeyEd25519` TEXT NOT NULL, `isTrustActive` INTEGER NOT NULL, `pairingTimestamp` INTEGER NOT NULL, `lastSeenTimestamp` INTEGER NOT NULL, PRIMARY KEY(`deviceId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `local_identity` (`idAlias` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `name` TEXT NOT NULL, `publicKeyEd25519` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`idAlias`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a67d99f0ca10f3c01f15ffd9574d4f92')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `trusted_devices`");
        db.execSQL("DROP TABLE IF EXISTS `local_identity`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTrustedDevices = new HashMap<String, TableInfo.Column>(8);
        _columnsTrustedDevices.put("deviceId", new TableInfo.Column("deviceId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustedDevices.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustedDevices.put("bleMacAddress", new TableInfo.Column("bleMacAddress", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustedDevices.put("lastKnownIpAddress", new TableInfo.Column("lastKnownIpAddress", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustedDevices.put("publicKeyEd25519", new TableInfo.Column("publicKeyEd25519", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustedDevices.put("isTrustActive", new TableInfo.Column("isTrustActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustedDevices.put("pairingTimestamp", new TableInfo.Column("pairingTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustedDevices.put("lastSeenTimestamp", new TableInfo.Column("lastSeenTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrustedDevices = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTrustedDevices = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTrustedDevices = new TableInfo("trusted_devices", _columnsTrustedDevices, _foreignKeysTrustedDevices, _indicesTrustedDevices);
        final TableInfo _existingTrustedDevices = TableInfo.read(db, "trusted_devices");
        if (!_infoTrustedDevices.equals(_existingTrustedDevices)) {
          return new RoomOpenHelper.ValidationResult(false, "trusted_devices(com.ecosystem.core.trusteddevices.TrustedDeviceEntity).\n"
                  + " Expected:\n" + _infoTrustedDevices + "\n"
                  + " Found:\n" + _existingTrustedDevices);
        }
        final HashMap<String, TableInfo.Column> _columnsLocalIdentity = new HashMap<String, TableInfo.Column>(5);
        _columnsLocalIdentity.put("idAlias", new TableInfo.Column("idAlias", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalIdentity.put("deviceId", new TableInfo.Column("deviceId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalIdentity.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalIdentity.put("publicKeyEd25519", new TableInfo.Column("publicKeyEd25519", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalIdentity.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocalIdentity = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocalIdentity = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocalIdentity = new TableInfo("local_identity", _columnsLocalIdentity, _foreignKeysLocalIdentity, _indicesLocalIdentity);
        final TableInfo _existingLocalIdentity = TableInfo.read(db, "local_identity");
        if (!_infoLocalIdentity.equals(_existingLocalIdentity)) {
          return new RoomOpenHelper.ValidationResult(false, "local_identity(com.ecosystem.core.identity.LocalIdentityEntity).\n"
                  + " Expected:\n" + _infoLocalIdentity + "\n"
                  + " Found:\n" + _existingLocalIdentity);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "a67d99f0ca10f3c01f15ffd9574d4f92", "41ad7cf7a3cb84d16e7dde8ebd3155b6");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "trusted_devices","local_identity");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `trusted_devices`");
      _db.execSQL("DELETE FROM `local_identity`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TrustedDeviceDao.class, TrustedDeviceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LocalIdentityDao.class, LocalIdentityDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TrustedDeviceDao trustedDeviceDao() {
    if (_trustedDeviceDao != null) {
      return _trustedDeviceDao;
    } else {
      synchronized(this) {
        if(_trustedDeviceDao == null) {
          _trustedDeviceDao = new TrustedDeviceDao_Impl(this);
        }
        return _trustedDeviceDao;
      }
    }
  }

  @Override
  public LocalIdentityDao localIdentityDao() {
    if (_localIdentityDao != null) {
      return _localIdentityDao;
    } else {
      synchronized(this) {
        if(_localIdentityDao == null) {
          _localIdentityDao = new LocalIdentityDao_Impl(this);
        }
        return _localIdentityDao;
      }
    }
  }
}

package com.ecosystem.core.trusteddevices;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TrustedDeviceDao_Impl implements TrustedDeviceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TrustedDeviceEntity> __insertionAdapterOfTrustedDeviceEntity;

  private final EntityDeletionOrUpdateAdapter<TrustedDeviceEntity> __deletionAdapterOfTrustedDeviceEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDeviceIpAddress;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastSeen;

  public TrustedDeviceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrustedDeviceEntity = new EntityInsertionAdapter<TrustedDeviceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `trusted_devices` (`deviceId`,`name`,`bleMacAddress`,`lastKnownIpAddress`,`publicKeyEd25519`,`isTrustActive`,`pairingTimestamp`,`lastSeenTimestamp`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrustedDeviceEntity entity) {
        if (entity.getDeviceId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getDeviceId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getBleMacAddress() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getBleMacAddress());
        }
        if (entity.getLastKnownIpAddress() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getLastKnownIpAddress());
        }
        if (entity.getPublicKeyEd25519() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getPublicKeyEd25519());
        }
        final int _tmp = entity.isTrustActive() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getPairingTimestamp());
        statement.bindLong(8, entity.getLastSeenTimestamp());
      }
    };
    this.__deletionAdapterOfTrustedDeviceEntity = new EntityDeletionOrUpdateAdapter<TrustedDeviceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `trusted_devices` WHERE `deviceId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrustedDeviceEntity entity) {
        if (entity.getDeviceId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getDeviceId());
        }
      }
    };
    this.__preparedStmtOfUpdateDeviceIpAddress = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE trusted_devices SET lastKnownIpAddress = ? WHERE deviceId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLastSeen = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE trusted_devices SET lastSeenTimestamp = ? WHERE deviceId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdateDevice(final TrustedDeviceEntity device,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTrustedDeviceEntity.insert(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDevice(final TrustedDeviceEntity device,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTrustedDeviceEntity.handle(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDeviceIpAddress(final String deviceId, final String ipAddress,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDeviceIpAddress.acquire();
        int _argIndex = 1;
        if (ipAddress == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, ipAddress);
        }
        _argIndex = 2;
        if (deviceId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, deviceId);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateDeviceIpAddress.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLastSeen(final String deviceId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastSeen.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        if (deviceId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, deviceId);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateLastSeen.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TrustedDeviceEntity>> getActiveTrustedDevicesFlow() {
    final String _sql = "SELECT * FROM trusted_devices WHERE isTrustActive = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"trusted_devices"}, new Callable<List<TrustedDeviceEntity>>() {
      @Override
      @NonNull
      public List<TrustedDeviceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBleMacAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "bleMacAddress");
          final int _cursorIndexOfLastKnownIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastKnownIpAddress");
          final int _cursorIndexOfPublicKeyEd25519 = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKeyEd25519");
          final int _cursorIndexOfIsTrustActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrustActive");
          final int _cursorIndexOfPairingTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingTimestamp");
          final int _cursorIndexOfLastSeenTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenTimestamp");
          final List<TrustedDeviceEntity> _result = new ArrayList<TrustedDeviceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrustedDeviceEntity _item;
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpBleMacAddress;
            if (_cursor.isNull(_cursorIndexOfBleMacAddress)) {
              _tmpBleMacAddress = null;
            } else {
              _tmpBleMacAddress = _cursor.getString(_cursorIndexOfBleMacAddress);
            }
            final String _tmpLastKnownIpAddress;
            if (_cursor.isNull(_cursorIndexOfLastKnownIpAddress)) {
              _tmpLastKnownIpAddress = null;
            } else {
              _tmpLastKnownIpAddress = _cursor.getString(_cursorIndexOfLastKnownIpAddress);
            }
            final String _tmpPublicKeyEd25519;
            if (_cursor.isNull(_cursorIndexOfPublicKeyEd25519)) {
              _tmpPublicKeyEd25519 = null;
            } else {
              _tmpPublicKeyEd25519 = _cursor.getString(_cursorIndexOfPublicKeyEd25519);
            }
            final boolean _tmpIsTrustActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsTrustActive);
            _tmpIsTrustActive = _tmp != 0;
            final long _tmpPairingTimestamp;
            _tmpPairingTimestamp = _cursor.getLong(_cursorIndexOfPairingTimestamp);
            final long _tmpLastSeenTimestamp;
            _tmpLastSeenTimestamp = _cursor.getLong(_cursorIndexOfLastSeenTimestamp);
            _item = new TrustedDeviceEntity(_tmpDeviceId,_tmpName,_tmpBleMacAddress,_tmpLastKnownIpAddress,_tmpPublicKeyEd25519,_tmpIsTrustActive,_tmpPairingTimestamp,_tmpLastSeenTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getActiveTrustedDevices(
      final Continuation<? super List<TrustedDeviceEntity>> $completion) {
    final String _sql = "SELECT * FROM trusted_devices WHERE isTrustActive = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TrustedDeviceEntity>>() {
      @Override
      @NonNull
      public List<TrustedDeviceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBleMacAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "bleMacAddress");
          final int _cursorIndexOfLastKnownIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastKnownIpAddress");
          final int _cursorIndexOfPublicKeyEd25519 = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKeyEd25519");
          final int _cursorIndexOfIsTrustActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrustActive");
          final int _cursorIndexOfPairingTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingTimestamp");
          final int _cursorIndexOfLastSeenTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenTimestamp");
          final List<TrustedDeviceEntity> _result = new ArrayList<TrustedDeviceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrustedDeviceEntity _item;
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpBleMacAddress;
            if (_cursor.isNull(_cursorIndexOfBleMacAddress)) {
              _tmpBleMacAddress = null;
            } else {
              _tmpBleMacAddress = _cursor.getString(_cursorIndexOfBleMacAddress);
            }
            final String _tmpLastKnownIpAddress;
            if (_cursor.isNull(_cursorIndexOfLastKnownIpAddress)) {
              _tmpLastKnownIpAddress = null;
            } else {
              _tmpLastKnownIpAddress = _cursor.getString(_cursorIndexOfLastKnownIpAddress);
            }
            final String _tmpPublicKeyEd25519;
            if (_cursor.isNull(_cursorIndexOfPublicKeyEd25519)) {
              _tmpPublicKeyEd25519 = null;
            } else {
              _tmpPublicKeyEd25519 = _cursor.getString(_cursorIndexOfPublicKeyEd25519);
            }
            final boolean _tmpIsTrustActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsTrustActive);
            _tmpIsTrustActive = _tmp != 0;
            final long _tmpPairingTimestamp;
            _tmpPairingTimestamp = _cursor.getLong(_cursorIndexOfPairingTimestamp);
            final long _tmpLastSeenTimestamp;
            _tmpLastSeenTimestamp = _cursor.getLong(_cursorIndexOfLastSeenTimestamp);
            _item = new TrustedDeviceEntity(_tmpDeviceId,_tmpName,_tmpBleMacAddress,_tmpLastKnownIpAddress,_tmpPublicKeyEd25519,_tmpIsTrustActive,_tmpPairingTimestamp,_tmpLastSeenTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDeviceById(final String deviceId,
      final Continuation<? super TrustedDeviceEntity> $completion) {
    final String _sql = "SELECT * FROM trusted_devices WHERE deviceId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (deviceId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, deviceId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TrustedDeviceEntity>() {
      @Override
      @Nullable
      public TrustedDeviceEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBleMacAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "bleMacAddress");
          final int _cursorIndexOfLastKnownIpAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastKnownIpAddress");
          final int _cursorIndexOfPublicKeyEd25519 = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKeyEd25519");
          final int _cursorIndexOfIsTrustActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrustActive");
          final int _cursorIndexOfPairingTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "pairingTimestamp");
          final int _cursorIndexOfLastSeenTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenTimestamp");
          final TrustedDeviceEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpBleMacAddress;
            if (_cursor.isNull(_cursorIndexOfBleMacAddress)) {
              _tmpBleMacAddress = null;
            } else {
              _tmpBleMacAddress = _cursor.getString(_cursorIndexOfBleMacAddress);
            }
            final String _tmpLastKnownIpAddress;
            if (_cursor.isNull(_cursorIndexOfLastKnownIpAddress)) {
              _tmpLastKnownIpAddress = null;
            } else {
              _tmpLastKnownIpAddress = _cursor.getString(_cursorIndexOfLastKnownIpAddress);
            }
            final String _tmpPublicKeyEd25519;
            if (_cursor.isNull(_cursorIndexOfPublicKeyEd25519)) {
              _tmpPublicKeyEd25519 = null;
            } else {
              _tmpPublicKeyEd25519 = _cursor.getString(_cursorIndexOfPublicKeyEd25519);
            }
            final boolean _tmpIsTrustActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsTrustActive);
            _tmpIsTrustActive = _tmp != 0;
            final long _tmpPairingTimestamp;
            _tmpPairingTimestamp = _cursor.getLong(_cursorIndexOfPairingTimestamp);
            final long _tmpLastSeenTimestamp;
            _tmpLastSeenTimestamp = _cursor.getLong(_cursorIndexOfLastSeenTimestamp);
            _result = new TrustedDeviceEntity(_tmpDeviceId,_tmpName,_tmpBleMacAddress,_tmpLastKnownIpAddress,_tmpPublicKeyEd25519,_tmpIsTrustActive,_tmpPairingTimestamp,_tmpLastSeenTimestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

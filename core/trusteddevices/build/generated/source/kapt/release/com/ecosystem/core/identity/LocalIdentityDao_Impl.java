package com.ecosystem.core.identity;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LocalIdentityDao_Impl implements LocalIdentityDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalIdentityEntity> __insertionAdapterOfLocalIdentityEntity;

  public LocalIdentityDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalIdentityEntity = new EntityInsertionAdapter<LocalIdentityEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `local_identity` (`idAlias`,`deviceId`,`name`,`publicKeyEd25519`,`createdAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalIdentityEntity entity) {
        if (entity.getIdAlias() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getIdAlias());
        }
        if (entity.getDeviceId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDeviceId());
        }
        if (entity.getName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getName());
        }
        if (entity.getPublicKeyEd25519() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPublicKeyEd25519());
        }
        statement.bindLong(5, entity.getCreatedAt());
      }
    };
  }

  @Override
  public Object insertIdentity(final LocalIdentityEntity identity,
      final Continuation<? super Unit> arg1) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalIdentityEntity.insert(identity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, arg1);
  }

  @Override
  public Object getIdentity(final String alias,
      final Continuation<? super LocalIdentityEntity> arg1) {
    final String _sql = "SELECT * FROM local_identity WHERE idAlias = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (alias == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, alias);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalIdentityEntity>() {
      @Override
      @Nullable
      public LocalIdentityEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdAlias = CursorUtil.getColumnIndexOrThrow(_cursor, "idAlias");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPublicKeyEd25519 = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKeyEd25519");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final LocalIdentityEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpIdAlias;
            if (_cursor.isNull(_cursorIndexOfIdAlias)) {
              _tmpIdAlias = null;
            } else {
              _tmpIdAlias = _cursor.getString(_cursorIndexOfIdAlias);
            }
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
            final String _tmpPublicKeyEd25519;
            if (_cursor.isNull(_cursorIndexOfPublicKeyEd25519)) {
              _tmpPublicKeyEd25519 = null;
            } else {
              _tmpPublicKeyEd25519 = _cursor.getString(_cursorIndexOfPublicKeyEd25519);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new LocalIdentityEntity(_tmpIdAlias,_tmpDeviceId,_tmpName,_tmpPublicKeyEd25519,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, arg1);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

package com.ecosystem.core.identity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalIdentityDao {
    @Query("SELECT * FROM local_identity WHERE idAlias = :alias LIMIT 1")
    suspend fun getIdentity(alias: String = "primary_device_identity"): LocalIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: LocalIdentityEntity)
}

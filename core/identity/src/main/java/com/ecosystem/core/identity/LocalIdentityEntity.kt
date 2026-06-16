package com.ecosystem.core.identity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_identity")
data class LocalIdentityEntity(
    @PrimaryKey 
    val idAlias: String = "primary_device_identity",
    val deviceId: String,
    val name: String,
    val publicKeyEd25519: String,
    val advertisingIdentifier: String,
    val createdAt: Long
)

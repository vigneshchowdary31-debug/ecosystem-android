package com.ecosystem.core.trusteddevices;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&\u00a8\u0006\u0007"}, d2 = {"Lcom/ecosystem/core/trusteddevices/AppDatabase;", "Landroidx/room/RoomDatabase;", "()V", "localIdentityDao", "Lcom/ecosystem/core/identity/LocalIdentityDao;", "trustedDeviceDao", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceDao;", "trusteddevices_debug"})
@androidx.room.Database(entities = {com.ecosystem.core.trusteddevices.TrustedDeviceEntity.class, com.ecosystem.core.identity.LocalIdentityEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends androidx.room.RoomDatabase {
    
    public AppDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.ecosystem.core.trusteddevices.TrustedDeviceDao trustedDeviceDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.ecosystem.core.identity.LocalIdentityDao localIdentityDao();
}
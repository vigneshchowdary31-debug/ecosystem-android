package com.ecosystem.core.trusteddevices.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \u00072\u00020\u0001:\u0001\u0007B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'\u00a8\u0006\b"}, d2 = {"Lcom/ecosystem/core/trusteddevices/di/DatabaseModule;", "", "()V", "bindTrustedDeviceRepository", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepository;", "impl", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepositoryImpl;", "Companion", "trusteddevices_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract class DatabaseModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.ecosystem.core.trusteddevices.di.DatabaseModule.Companion Companion = null;
    
    public DatabaseModule() {
        super();
    }
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.ecosystem.core.trusteddevices.TrustedDeviceRepository bindTrustedDeviceRepository(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.trusteddevices.TrustedDeviceRepositoryImpl impl);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\t\u001a\u00020\u0004H\u0007\u00a8\u0006\f"}, d2 = {"Lcom/ecosystem/core/trusteddevices/di/DatabaseModule$Companion;", "", "()V", "provideAppDatabase", "Lcom/ecosystem/core/trusteddevices/AppDatabase;", "context", "Landroid/content/Context;", "provideLocalIdentityDao", "Lcom/ecosystem/core/identity/LocalIdentityDao;", "database", "provideTrustedDeviceDao", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceDao;", "trusteddevices_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @dagger.Provides()
        @javax.inject.Singleton()
        @org.jetbrains.annotations.NotNull()
        public final com.ecosystem.core.trusteddevices.AppDatabase provideAppDatabase(@dagger.hilt.android.qualifiers.ApplicationContext()
        @org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
        
        @dagger.Provides()
        @javax.inject.Singleton()
        @org.jetbrains.annotations.NotNull()
        public final com.ecosystem.core.trusteddevices.TrustedDeviceDao provideTrustedDeviceDao(@org.jetbrains.annotations.NotNull()
        com.ecosystem.core.trusteddevices.AppDatabase database) {
            return null;
        }
        
        @dagger.Provides()
        @javax.inject.Singleton()
        @org.jetbrains.annotations.NotNull()
        public final com.ecosystem.core.identity.LocalIdentityDao provideLocalIdentityDao(@org.jetbrains.annotations.NotNull()
        com.ecosystem.core.trusteddevices.AppDatabase database) {
            return null;
        }
    }
}
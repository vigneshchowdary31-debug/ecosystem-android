package com.ecosystem.core.identity;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0012\n\u0002\b\u0004\b\u0007\u0018\u0000 \u00132\u00020\u0001:\u0001\u0013B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\b\u0010\t\u001a\u00020\nH\u0002J\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fH\u0096@\u00a2\u0006\u0002\u0010\rJ\u000e\u0010\u000e\u001a\u00020\fH\u0096@\u00a2\u0006\u0002\u0010\rJ\u0016\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0010H\u0096@\u00a2\u0006\u0002\u0010\u0012R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/ecosystem/core/identity/IdentityManagerImpl;", "Lcom/ecosystem/core/identity/IdentityManager;", "localIdentityDao", "Lcom/ecosystem/core/identity/LocalIdentityDao;", "cryptoManager", "Lcom/ecosystem/core/security/CryptoManager;", "secureStorage", "Lcom/ecosystem/core/security/SecureStorage;", "(Lcom/ecosystem/core/identity/LocalIdentityDao;Lcom/ecosystem/core/security/CryptoManager;Lcom/ecosystem/core/security/SecureStorage;)V", "getDeviceName", "", "getIdentity", "Lcom/ecosystem/core/identity/DeviceInfo;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getOrCreateIdentity", "signChallenge", "", "challenge", "([BLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "identity_debug"})
public final class IdentityManagerImpl implements com.ecosystem.core.identity.IdentityManager {
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.identity.LocalIdentityDao localIdentityDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.CryptoManager cryptoManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.SecureStorage secureStorage = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String IDENTITY_ALIAS = "primary_device_identity";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SECURE_DEVICE_ID = "identity_device_id";
    @org.jetbrains.annotations.NotNull()
    public static final com.ecosystem.core.identity.IdentityManagerImpl.Companion Companion = null;
    
    @javax.inject.Inject()
    public IdentityManagerImpl(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.identity.LocalIdentityDao localIdentityDao, @org.jetbrains.annotations.NotNull()
    com.ecosystem.core.security.CryptoManager cryptoManager, @org.jetbrains.annotations.NotNull()
    com.ecosystem.core.security.SecureStorage secureStorage) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getOrCreateIdentity(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ecosystem.core.identity.DeviceInfo> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getIdentity(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ecosystem.core.identity.DeviceInfo> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object signChallenge(@org.jetbrains.annotations.NotNull()
    byte[] challenge, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super byte[]> $completion) {
        return null;
    }
    
    private final java.lang.String getDeviceName() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/ecosystem/core/identity/IdentityManagerImpl$Companion;", "", "()V", "IDENTITY_ALIAS", "", "KEY_SECURE_DEVICE_ID", "identity_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}
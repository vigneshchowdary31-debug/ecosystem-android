package com.ecosystem.android;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0014R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001e\u0010\t\u001a\u00020\n8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001e\u0010\u000f\u001a\u00020\u00108\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\u0012\"\u0004\b\u0013\u0010\u0014\u00a8\u0006\u0019"}, d2 = {"Lcom/ecosystem/android/MainActivity;", "Landroidx/activity/ComponentActivity;", "()V", "discoveryService", "Lcom/ecosystem/core/discovery/domain/repository/DiscoveryService;", "getDiscoveryService", "()Lcom/ecosystem/core/discovery/domain/repository/DiscoveryService;", "setDiscoveryService", "(Lcom/ecosystem/core/discovery/domain/repository/DiscoveryService;)V", "identityManager", "Lcom/ecosystem/core/identity/IdentityManager;", "getIdentityManager", "()Lcom/ecosystem/core/identity/IdentityManager;", "setIdentityManager", "(Lcom/ecosystem/core/identity/IdentityManager;)V", "trustedDeviceRepository", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepository;", "getTrustedDeviceRepository", "()Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepository;", "setTrustedDeviceRepository", "(Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepository;)V", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "app_release"})
public final class MainActivity extends androidx.activity.ComponentActivity {
    @javax.inject.Inject()
    public com.ecosystem.core.identity.IdentityManager identityManager;
    @javax.inject.Inject()
    public com.ecosystem.core.discovery.domain.repository.DiscoveryService discoveryService;
    @javax.inject.Inject()
    public com.ecosystem.core.trusteddevices.TrustedDeviceRepository trustedDeviceRepository;
    
    public MainActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecosystem.core.identity.IdentityManager getIdentityManager() {
        return null;
    }
    
    public final void setIdentityManager(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.identity.IdentityManager p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecosystem.core.discovery.domain.repository.DiscoveryService getDiscoveryService() {
        return null;
    }
    
    public final void setDiscoveryService(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.discovery.domain.repository.DiscoveryService p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ecosystem.core.trusteddevices.TrustedDeviceRepository getTrustedDeviceRepository() {
        return null;
    }
    
    public final void setTrustedDeviceRepository(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.trusteddevices.TrustedDeviceRepository p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
}
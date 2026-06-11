package com.ecosystem.android;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\u001a\"\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u0007\u001a\u0018\u0010\b\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007\u001a \u0010\r\u001a\u00020\u00012\u0006\u0010\u000e\u001a\u00020\f2\u0006\u0010\u000f\u001a\u00020\f2\u0006\u0010\u0010\u001a\u00020\u0011H\u0007\u00a8\u0006\u0012"}, d2 = {"BleDebugScreen", "", "identityManager", "Lcom/ecosystem/core/identity/IdentityManager;", "discoveryService", "Lcom/ecosystem/core/discovery/domain/repository/DiscoveryService;", "modifier", "Landroidx/compose/ui/Modifier;", "DeviceListItem", "device", "Lcom/ecosystem/core/discovery/domain/model/DiscoveredDevice;", "lastSeen", "", "StatusRow", "label", "value", "isActive", "", "app_release"})
public final class BleDebugScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void BleDebugScreen(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.identity.IdentityManager identityManager, @org.jetbrains.annotations.NotNull()
    com.ecosystem.core.discovery.domain.repository.DiscoveryService discoveryService, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void StatusRow(@org.jetbrains.annotations.NotNull()
    java.lang.String label, @org.jetbrains.annotations.NotNull()
    java.lang.String value, boolean isActive) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void DeviceListItem(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.discovery.domain.model.DiscoveredDevice device, @org.jetbrains.annotations.NotNull()
    java.lang.String lastSeen) {
    }
}
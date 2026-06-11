package com.ecosystem.core.discovery.domain.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u000eH&J\u0014\u0010\u0010\u001a\u00020\f2\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u000eH&J\b\u0010\u0012\u001a\u00020\fH&J\b\u0010\u0013\u001a\u00020\fH&R\u0018\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0002\u0010\u0005R\u0018\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0005R\u001e\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u0005\u00a8\u0006\u0014"}, d2 = {"Lcom/ecosystem/core/discovery/domain/repository/DiscoveryService;", "", "isAdvertising", "Lkotlinx/coroutines/flow/Flow;", "", "()Lkotlinx/coroutines/flow/Flow;", "isScanning", "scanResults", "", "Lcom/ecosystem/core/discovery/domain/model/DiscoveredDevice;", "getScanResults", "startAdvertising", "", "localDeviceId", "", "localName", "startScanning", "serviceUuid", "stopAdvertising", "stopScanning", "discovery_debug"})
public abstract interface DiscoveryService {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.ecosystem.core.discovery.domain.model.DiscoveredDevice>> getScanResults();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Boolean> isScanning();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Boolean> isAdvertising();
    
    public abstract void startScanning(@org.jetbrains.annotations.Nullable()
    java.lang.String serviceUuid);
    
    public abstract void stopScanning();
    
    public abstract void startAdvertising(@org.jetbrains.annotations.NotNull()
    java.lang.String localDeviceId, @org.jetbrains.annotations.NotNull()
    java.lang.String localName);
    
    public abstract void stopAdvertising();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}
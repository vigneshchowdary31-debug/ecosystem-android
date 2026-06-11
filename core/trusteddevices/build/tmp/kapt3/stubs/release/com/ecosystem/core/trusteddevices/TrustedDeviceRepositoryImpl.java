package com.ecosystem.core.trusteddevices;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\t\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u0010\tJ\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\b0\u000bH\u0096@\u00a2\u0006\u0002\u0010\fJ\u0014\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u000b0\u000eH\u0016J\u0018\u0010\u000f\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0010\u001a\u00020\u0011H\u0096@\u00a2\u0006\u0002\u0010\u0012J\u0016\u0010\u0013\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u0010\tJ \u0010\u0014\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\b\u0010\u0015\u001a\u0004\u0018\u00010\u0011H\u0096@\u00a2\u0006\u0002\u0010\u0016J\u001e\u0010\u0017\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0018\u001a\u00020\u0019H\u0096@\u00a2\u0006\u0002\u0010\u001aR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepositoryImpl;", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepository;", "trustedDeviceDao", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceDao;", "(Lcom/ecosystem/core/trusteddevices/TrustedDeviceDao;)V", "addTrustedDevice", "", "device", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceEntity;", "(Lcom/ecosystem/core/trusteddevices/TrustedDeviceEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getActiveTrustedDevices", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getActiveTrustedDevicesFlow", "Lkotlinx/coroutines/flow/Flow;", "getDeviceById", "deviceId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "removeTrustedDevice", "updateDeviceIpAddress", "ipAddress", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateLastSeen", "timestamp", "", "(Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "trusteddevices_release"})
public final class TrustedDeviceRepositoryImpl implements com.ecosystem.core.trusteddevices.TrustedDeviceRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.trusteddevices.TrustedDeviceDao trustedDeviceDao = null;
    
    @javax.inject.Inject()
    public TrustedDeviceRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.trusteddevices.TrustedDeviceDao trustedDeviceDao) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.ecosystem.core.trusteddevices.TrustedDeviceEntity>> getActiveTrustedDevicesFlow() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getActiveTrustedDevices(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.ecosystem.core.trusteddevices.TrustedDeviceEntity>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getDeviceById(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ecosystem.core.trusteddevices.TrustedDeviceEntity> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object addTrustedDevice(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.trusteddevices.TrustedDeviceEntity device, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object removeTrustedDevice(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.trusteddevices.TrustedDeviceEntity device, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateDeviceIpAddress(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.Nullable()
    java.lang.String ipAddress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateLastSeen(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, long timestamp, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}
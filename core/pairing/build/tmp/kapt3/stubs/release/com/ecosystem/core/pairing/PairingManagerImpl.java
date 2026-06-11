package com.ecosystem.core.pairing;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\'\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0012\u0010\u0012\u001a\u0004\u0018\u00010\u00132\u0006\u0010\u0014\u001a\u00020\u0015H\u0002J\b\u0010\u0016\u001a\u00020\u0017H\u0016J\u0016\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u0015H\u0096@\u00a2\u0006\u0002\u0010\u001bR\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\r0\u000fX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/ecosystem/core/pairing/PairingManagerImpl;", "Lcom/ecosystem/core/pairing/PairingManager;", "bleChannel", "Lcom/ecosystem/core/networking/BleChannel;", "cryptoManager", "Lcom/ecosystem/core/security/CryptoManager;", "identityManager", "Lcom/ecosystem/core/identity/IdentityManager;", "trustedDeviceRepository", "Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepository;", "(Lcom/ecosystem/core/networking/BleChannel;Lcom/ecosystem/core/security/CryptoManager;Lcom/ecosystem/core/identity/IdentityManager;Lcom/ecosystem/core/trusteddevices/TrustedDeviceRepository;)V", "_pairingState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/ecosystem/core/pairing/PairingState;", "pairingState", "Lkotlinx/coroutines/flow/StateFlow;", "getPairingState", "()Lkotlinx/coroutines/flow/StateFlow;", "parseQrPayload", "Lcom/ecosystem/core/pairing/QrPairingData;", "payload", "", "reset", "", "startPairing", "", "qrPayload", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pairing_release"})
public final class PairingManagerImpl implements com.ecosystem.core.pairing.PairingManager {
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.networking.BleChannel bleChannel = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.CryptoManager cryptoManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.identity.IdentityManager identityManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.trusteddevices.TrustedDeviceRepository trustedDeviceRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.ecosystem.core.pairing.PairingState> _pairingState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ecosystem.core.pairing.PairingState> pairingState = null;
    
    @javax.inject.Inject()
    public PairingManagerImpl(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.networking.BleChannel bleChannel, @org.jetbrains.annotations.NotNull()
    com.ecosystem.core.security.CryptoManager cryptoManager, @org.jetbrains.annotations.NotNull()
    com.ecosystem.core.identity.IdentityManager identityManager, @org.jetbrains.annotations.NotNull()
    com.ecosystem.core.trusteddevices.TrustedDeviceRepository trustedDeviceRepository) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.StateFlow<com.ecosystem.core.pairing.PairingState> getPairingState() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object startPairing(@org.jetbrains.annotations.NotNull()
    java.lang.String qrPayload, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @java.lang.Override()
    public void reset() {
    }
    
    private final com.ecosystem.core.pairing.QrPairingData parseQrPayload(java.lang.String payload) {
        return null;
    }
}
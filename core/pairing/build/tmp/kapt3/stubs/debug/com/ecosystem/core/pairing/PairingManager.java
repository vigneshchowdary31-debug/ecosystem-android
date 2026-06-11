package com.ecosystem.core.pairing;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\b\u0010\u0007\u001a\u00020\bH&J\u0016\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u00a6@\u00a2\u0006\u0002\u0010\rR\u0018\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u000e"}, d2 = {"Lcom/ecosystem/core/pairing/PairingManager;", "", "pairingState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/ecosystem/core/pairing/PairingState;", "getPairingState", "()Lkotlinx/coroutines/flow/StateFlow;", "reset", "", "startPairing", "", "qrPayload", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pairing_debug"})
public abstract interface PairingManager {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.StateFlow<com.ecosystem.core.pairing.PairingState> getPairingState();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object startPairing(@org.jetbrains.annotations.NotNull()
    java.lang.String qrPayload, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    public abstract void reset();
}
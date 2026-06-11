package com.ecosystem.core.identity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0012\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u0004\u0018\u00010\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u0007H\u00a6@\u00a2\u0006\u0002\u0010\t\u00a8\u0006\n"}, d2 = {"Lcom/ecosystem/core/identity/IdentityManager;", "", "getIdentity", "Lcom/ecosystem/core/identity/DeviceInfo;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getOrCreateIdentity", "signChallenge", "", "challenge", "([BLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "identity_release"})
public abstract interface IdentityManager {
    
    /**
     * Retrieves the existing device identity or generates a new one.
     * Generates a unique secure UUID on first launch.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getOrCreateIdentity(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ecosystem.core.identity.DeviceInfo> $completion);
    
    /**
     * Retrieves the current device identity, or null if none exists.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getIdentity(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ecosystem.core.identity.DeviceInfo> $completion);
    
    /**
     * Signs a challenge byte array using the local device's long-term private key.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object signChallenge(@org.jetbrains.annotations.NotNull()
    byte[] challenge, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super byte[]> $completion);
}
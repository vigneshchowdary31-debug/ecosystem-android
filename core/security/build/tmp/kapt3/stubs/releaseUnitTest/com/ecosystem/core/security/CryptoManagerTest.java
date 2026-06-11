package com.ecosystem.core.security;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0007\u001a\u00020\bH\u0007J\b\u0010\t\u001a\u00020\bH\u0007J\b\u0010\n\u001a\u00020\bH\u0007J\b\u0010\u000b\u001a\u00020\bH\u0007R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/ecosystem/core/security/CryptoManagerTest;", "", "()V", "cryptoManager", "Lcom/ecosystem/core/security/CryptoManager;", "secureStorage", "Lcom/ecosystem/core/security/SecureStorage;", "computeSharedSessionKey generates identical keys for both peers", "", "generateIdentityKeyPair generates and saves public and private keys", "setUp", "signAndVerify succeeds with generated keys", "security_releaseUnitTest"})
public final class CryptoManagerTest {
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.SecureStorage secureStorage = null;
    private com.ecosystem.core.security.CryptoManager cryptoManager;
    
    public CryptoManagerTest() {
        super();
    }
    
    @org.junit.Before()
    public final void setUp() {
    }
}
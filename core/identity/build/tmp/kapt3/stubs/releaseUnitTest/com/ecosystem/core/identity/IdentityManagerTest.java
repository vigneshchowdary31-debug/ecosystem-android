package com.ecosystem.core.identity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\f\u0010\u000b\u001a\u00060\fj\u0002`\rH\u0007J\f\u0010\u000e\u001a\u00060\fj\u0002`\rH\u0007J\f\u0010\u000f\u001a\u00060\fj\u0002`\rH\u0007J\b\u0010\u0010\u001a\u00020\fH\u0007J\f\u0010\u0011\u001a\u00060\fj\u0002`\rH\u0007R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/ecosystem/core/identity/IdentityManagerTest;", "", "()V", "cryptoManager", "Lcom/ecosystem/core/security/CryptoManager;", "identityManager", "Lcom/ecosystem/core/identity/IdentityManager;", "localIdentityDao", "Lcom/ecosystem/core/identity/LocalIdentityDao;", "secureStorage", "Lcom/ecosystem/core/security/SecureStorage;", "getIdentity returns null when no identity exists", "", "Lkotlinx/coroutines/test/TestResult;", "getOrCreateIdentity generates new UUID and keys on first launch", "getOrCreateIdentity returns existing identity on subsequent launches", "setUp", "signChallenge delegates challenge signing to CryptoManager", "identity_releaseUnitTest"})
public final class IdentityManagerTest {
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.identity.LocalIdentityDao localIdentityDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.CryptoManager cryptoManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.SecureStorage secureStorage = null;
    private com.ecosystem.core.identity.IdentityManager identityManager;
    
    public IdentityManagerTest() {
        super();
    }
    
    @org.junit.Before()
    public final void setUp() {
    }
}
package com.ecosystem.core.security;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\r\u001a\u00020\u000eH\u0007J\b\u0010\u000f\u001a\u00020\u000eH\u0007J\b\u0010\u0010\u001a\u00020\u000eH\u0007J\b\u0010\u0011\u001a\u00020\u000eH\u0007R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/ecosystem/core/security/SecureStorageTest;", "", "()V", "context", "Landroid/content/Context;", "editor", "Landroid/content/SharedPreferences$Editor;", "keystoreManager", "Lcom/ecosystem/core/security/KeystoreManager;", "secureStorage", "Lcom/ecosystem/core/security/SecureStorage;", "sharedPreferences", "Landroid/content/SharedPreferences;", "getString retrieves and decrypts value", "", "getString returns null when key not found", "putString encrypts and saves value", "setUp", "security_debugUnitTest"})
public final class SecureStorageTest {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.KeystoreManager keystoreManager = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences sharedPreferences = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences.Editor editor = null;
    private com.ecosystem.core.security.SecureStorage secureStorage;
    
    public SecureStorageTest() {
        super();
    }
    
    @org.junit.Before()
    public final void setUp() {
    }
}
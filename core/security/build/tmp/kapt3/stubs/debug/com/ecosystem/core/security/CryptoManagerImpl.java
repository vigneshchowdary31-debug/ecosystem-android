package com.ecosystem.core.security;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\tH\u0016J\u0014\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\t0\u000bH\u0016J\u0010\u0010\f\u001a\u00020\t2\u0006\u0010\r\u001a\u00020\tH\u0016J\u0012\u0010\u000e\u001a\u0004\u0018\u00010\t2\u0006\u0010\r\u001a\u00020\tH\u0016J\u0018\u0010\u000f\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\t2\u0006\u0010\u0010\u001a\u00020\u0006H\u0016J \u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\t2\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0014\u001a\u00020\u0006H\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"Lcom/ecosystem/core/security/CryptoManagerImpl;", "Lcom/ecosystem/core/security/CryptoManager;", "secureStorage", "Lcom/ecosystem/core/security/SecureStorage;", "(Lcom/ecosystem/core/security/SecureStorage;)V", "computeSharedSessionKey", "", "localPrivateKey", "peerPublicKeyBase64", "", "generateEphemeralKeyPair", "Lkotlin/Pair;", "generateIdentityKeyPair", "alias", "getIdentityPublicKey", "signWithIdentityKey", "data", "verifySignature", "", "publicKeyBase64", "signature", "security_debug"})
public final class CryptoManagerImpl implements com.ecosystem.core.security.CryptoManager {
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.SecureStorage secureStorage = null;
    
    @javax.inject.Inject()
    public CryptoManagerImpl(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.security.SecureStorage secureStorage) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String generateIdentityKeyPair(@org.jetbrains.annotations.NotNull()
    java.lang.String alias) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public byte[] signWithIdentityKey(@org.jetbrains.annotations.NotNull()
    java.lang.String alias, @org.jetbrains.annotations.NotNull()
    byte[] data) {
        return null;
    }
    
    @java.lang.Override()
    public boolean verifySignature(@org.jetbrains.annotations.NotNull()
    java.lang.String publicKeyBase64, @org.jetbrains.annotations.NotNull()
    byte[] data, @org.jetbrains.annotations.NotNull()
    byte[] signature) {
        return false;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.String getIdentityPublicKey(@org.jetbrains.annotations.NotNull()
    java.lang.String alias) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlin.Pair<byte[], java.lang.String> generateEphemeralKeyPair() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public byte[] computeSharedSessionKey(@org.jetbrains.annotations.NotNull()
    byte[] localPrivateKey, @org.jetbrains.annotations.NotNull()
    java.lang.String peerPublicKeyBase64) {
        return null;
    }
}
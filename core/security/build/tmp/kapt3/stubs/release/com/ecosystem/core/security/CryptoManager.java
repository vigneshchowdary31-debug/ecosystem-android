package com.ecosystem.core.security;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0006H&J\u0014\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00060\bH&J\u0010\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u0006H&J\u0012\u0010\u000b\u001a\u0004\u0018\u00010\u00062\u0006\u0010\n\u001a\u00020\u0006H&J\u0018\u0010\f\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\u0003H&J \u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\u0003H&\u00a8\u0006\u0012"}, d2 = {"Lcom/ecosystem/core/security/CryptoManager;", "", "computeSharedSessionKey", "", "localPrivateKey", "peerPublicKeyBase64", "", "generateEphemeralKeyPair", "Lkotlin/Pair;", "generateIdentityKeyPair", "alias", "getIdentityPublicKey", "signWithIdentityKey", "data", "verifySignature", "", "publicKeyBase64", "signature", "security_release"})
public abstract interface CryptoManager {
    
    /**
     * Generates a long-term Ed25519 key pair for device identity.
     * The private key is securely stored (encrypted via Android Keystore AES-GCM).
     * Returns the Base64-encoded public key.
     */
    @org.jetbrains.annotations.NotNull()
    public abstract java.lang.String generateIdentityKeyPair(@org.jetbrains.annotations.NotNull()
    java.lang.String alias);
    
    /**
     * Signs data using the Ed25519 private key identified by the alias.
     */
    @org.jetbrains.annotations.NotNull()
    public abstract byte[] signWithIdentityKey(@org.jetbrains.annotations.NotNull()
    java.lang.String alias, @org.jetbrains.annotations.NotNull()
    byte[] data);
    
    /**
     * Verifies an Ed25519 signature using the peer's public key.
     */
    public abstract boolean verifySignature(@org.jetbrains.annotations.NotNull()
    java.lang.String publicKeyBase64, @org.jetbrains.annotations.NotNull()
    byte[] data, @org.jetbrains.annotations.NotNull()
    byte[] signature);
    
    /**
     * Gets the Base64-encoded public key for the local identity.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.String getIdentityPublicKey(@org.jetbrains.annotations.NotNull()
    java.lang.String alias);
    
    /**
     * Generates a local ephemeral X25519 key pair for DH key agreement.
     * Returns a pair of: (localPrivateRawBytes, localPublicBase64)
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlin.Pair<byte[], java.lang.String> generateEphemeralKeyPair();
    
    /**
     * Computes the shared secret (AES session key) using the local ephemeral private key
     * and the peer's ephemeral public key.
     */
    @org.jetbrains.annotations.NotNull()
    public abstract byte[] computeSharedSessionKey(@org.jetbrains.annotations.NotNull()
    byte[] localPrivateKey, @org.jetbrains.annotations.NotNull()
    java.lang.String peerPublicKeyBase64);
}
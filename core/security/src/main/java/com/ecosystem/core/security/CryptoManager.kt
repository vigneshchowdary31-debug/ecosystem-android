package com.ecosystem.core.security

interface CryptoManager {
    /**
     * Generates a long-term Ed25519 key pair for device identity.
     * The private key is securely stored (encrypted via Android Keystore AES-GCM).
     * Returns the Base64-encoded public key.
     */
    fun generateIdentityKeyPair(alias: String): String

    /**
     * Signs data using the Ed25519 private key identified by the alias.
     */
    fun signWithIdentityKey(alias: String, data: ByteArray): ByteArray

    /**
     * Verifies an Ed25519 signature using the peer's public key.
     */
    fun verifySignature(publicKeyBase64: String, data: ByteArray, signature: ByteArray): Boolean

    /**
     * Gets the Base64-encoded public key for the local identity.
     */
    fun getIdentityPublicKey(alias: String): String?

    /**
     * Generates a local ephemeral X25519 key pair for DH key agreement.
     * Returns a pair of: (localPrivateRawBytes, localPublicBase64)
     */
    fun generateEphemeralKeyPair(): Pair<ByteArray, String>

    /**
     * Computes the shared secret (AES session key) using the local ephemeral private key
     * and the peer's ephemeral public key.
     */
    fun computeSharedSessionKey(localPrivateKey: ByteArray, peerPublicKeyBase64: String): ByteArray

    /**
     * Computes HMAC-SHA256 of the data using the specified key.
     */
    fun computeHmacSha256(key: ByteArray, data: ByteArray): ByteArray
}

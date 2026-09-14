package com.ecosystem.core.security

/**
 * Long-term Ed25519 identity keys. Pairing-session cryptography (X25519, HKDF, HMAC,
 * signature verification) lives in `:core:protocol`.
 */
interface CryptoManager {
    /**
     * Generates a new Ed25519 key pair under [alias], stores the 32-byte private seed
     * encrypted at rest and returns the 32-byte public key. Replaces any existing key.
     */
    fun generateIdentityKeyPair(alias: String): ByteArray

    /** Reports whether a usable key exists, deriving the public key from the stored private key. */
    fun identityKeyState(alias: String): IdentityKeyState

    /** Signs [data]. The private key is decrypted into a byte array and zeroed right after. */
    fun sign(alias: String, data: ByteArray): ByteArray

    fun deleteIdentityKey(alias: String)
}

sealed interface IdentityKeyState {
    class Present(val publicKey: ByteArray) : IdentityKeyState
    data object Missing : IdentityKeyState
    class Unreadable(val cause: Throwable) : IdentityKeyState
}

/** The identity key exists but cannot be used (Keystore key lost, corrupted value) or is missing when needed. */
class IdentityKeyUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

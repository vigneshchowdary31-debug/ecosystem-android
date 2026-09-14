package com.ecosystem.core.identity

import com.ecosystem.core.security.IdentityKeyUnavailableException

interface IdentityManager {
    /**
     * Returns this device's identity, creating it on first launch. Creation is serialised, so
     * concurrent callers always get the same identity.
     *
     * Throws [IdentityKeyUnavailableException] if identity material exists but cannot be
     * decrypted. The identity is never replaced silently; see [resetIdentity].
     */
    suspend fun getOrCreateIdentity(): DeviceInfo

    /** Returns the identity if it exists, without creating one. */
    suspend fun getIdentity(): DeviceInfo?

    /** Signs [data] with the long-term Ed25519 identity key. */
    suspend fun sign(data: ByteArray): ByteArray

    /**
     * Deletes the identity and creates a new one. Macs paired with the old identity will no
     * longer recognise this device and must be paired again.
     */
    suspend fun resetIdentity(): DeviceInfo
}

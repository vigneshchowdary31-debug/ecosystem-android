package com.ecosystem.core.identity

interface IdentityManager {
    /**
     * Retrieves the existing device identity or generates a new one.
     * Generates a unique secure UUID on first launch.
     */
    suspend fun getOrCreateIdentity(): DeviceInfo

    /**
     * Retrieves the current device identity, or null if none exists.
     */
    suspend fun getIdentity(): DeviceInfo?

    /**
     * Signs a challenge byte array using the local device's long-term private key.
     */
    suspend fun signChallenge(challenge: ByteArray): ByteArray
}

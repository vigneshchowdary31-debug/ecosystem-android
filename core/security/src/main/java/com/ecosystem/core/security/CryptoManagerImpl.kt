package com.ecosystem.core.security

import com.google.crypto.tink.subtle.Ed25519Sign
import java.security.GeneralSecurityException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ed25519 identity keys generated with Tink (software) and stored as a raw 32-byte seed
 * sealed by [SecureStorage]. The seed exists in plaintext only inside [sign] and
 * [identityKeyState], as a byte array that is zeroed before returning. Tink's signer keeps
 * a derived scalar internally that cannot be wiped; it becomes garbage right after use.
 */
@Singleton
class CryptoManagerImpl @Inject constructor(
    private val secureStorage: SecureStorage,
) : CryptoManager {

    override fun generateIdentityKeyPair(alias: String): ByteArray {
        val keyPair = Ed25519Sign.KeyPair.newKeyPair()
        val seed = keyPair.privateKey
        try {
            secureStorage.putBytes(privateKeyName(alias), seed)
        } finally {
            seed.fill(0)
        }
        secureStorage.putString(publicKeyName(alias), Base64.getEncoder().encodeToString(keyPair.publicKey))
        return keyPair.publicKey
    }

    override fun identityKeyState(alias: String): IdentityKeyState =
        when (val read = secureStorage.readBytes(privateKeyName(alias))) {
            SecureRead.Missing -> IdentityKeyState.Missing
            is SecureRead.Unreadable -> IdentityKeyState.Unreadable(read.cause)
            is SecureRead.Present -> try {
                val seed = seedFrom(alias, read)
                try {
                    IdentityKeyState.Present(Ed25519Sign.KeyPair.newKeyPairFromSeed(seed).publicKey)
                } finally {
                    seed.fill(0)
                }
            } catch (e: IdentityKeyUnavailableException) {
                IdentityKeyState.Unreadable(e)
            } catch (e: GeneralSecurityException) {
                IdentityKeyState.Unreadable(e)
            }
        }

    override fun sign(alias: String, data: ByteArray): ByteArray {
        val seed = when (val read = secureStorage.readBytes(privateKeyName(alias))) {
            SecureRead.Missing -> throw IdentityKeyUnavailableException("No identity key for '$alias'")
            is SecureRead.Unreadable ->
                throw IdentityKeyUnavailableException("Identity key for '$alias' cannot be decrypted", read.cause)
            is SecureRead.Present -> seedFrom(alias, read)
        }
        try {
            return Ed25519Sign(seed).sign(data)
        } catch (e: GeneralSecurityException) {
            throw IdentityKeyUnavailableException("Identity key for '$alias' is invalid", e)
        } finally {
            seed.fill(0)
        }
    }

    override fun deleteIdentityKey(alias: String) {
        secureStorage.remove(privateKeyName(alias))
        secureStorage.remove(publicKeyName(alias))
    }

    /** Returns the raw seed. Seeds written before v2 were stored as Base64 text; they are re-stored as raw bytes. */
    private fun seedFrom(alias: String, read: SecureRead.Present): ByteArray {
        if (!read.isLegacyFormat) {
            if (read.value.size != SEED_SIZE) {
                read.wipe()
                throw IdentityKeyUnavailableException("Stored identity key has ${read.value.size} bytes")
            }
            return read.value
        }
        // One-time migration. The legacy value is Base64 text and becomes an immutable String
        // briefly; afterwards the key only exists as raw bytes.
        val legacyText = String(read.value, Charsets.US_ASCII).trim()
        read.wipe()
        val seed = try {
            Base64.getDecoder().decode(legacyText)
        } catch (e: IllegalArgumentException) {
            throw IdentityKeyUnavailableException("Legacy identity key is not valid Base64", e)
        }
        if (seed.size != SEED_SIZE) {
            seed.fill(0)
            throw IdentityKeyUnavailableException("Legacy identity key has ${seed.size} bytes")
        }
        secureStorage.putBytes(privateKeyName(alias), seed)
        return seed
    }

    private fun privateKeyName(alias: String) = "${alias}_prv"
    private fun publicKeyName(alias: String) = "${alias}_pub"

    private companion object {
        const val SEED_SIZE = 32
    }
}

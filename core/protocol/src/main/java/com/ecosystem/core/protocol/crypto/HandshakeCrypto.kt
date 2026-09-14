package com.ecosystem.core.protocol.crypto

import com.ecosystem.core.protocol.ProtocolConstants
import com.ecosystem.core.protocol.util.isAllZero
import com.ecosystem.core.protocol.util.wipe
import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.X25519
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** The primitives the pairing handshake needs. Identity signing is separate, see [IdentitySigner]. */
interface HandshakeCrypto {
    /** Cryptographically secure random bytes. */
    fun randomBytes(size: Int): ByteArray

    fun generateX25519KeyPair(): X25519KeyPair

    /** X25519 agreement. Throws [InvalidPeerKeyException] for a malformed peer key or an all-zero secret. */
    fun x25519(privateKey: ByteArray, peerPublicKey: ByteArray): ByteArray

    fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

    fun sha256(data: ByteArray): ByteArray

    fun verifyEd25519(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean
}

class X25519KeyPair(val privateKey: ByteArray, val publicKey: ByteArray) {
    fun wipePrivateKey() = privateKey.wipe()
}

class InvalidPeerKeyException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Signs with a long-term Ed25519 identity key without exposing the key to the protocol code. */
fun interface IdentitySigner {
    suspend fun sign(message: ByteArray): ByteArray
}

/**
 * [HandshakeCrypto] backed by Tink's RFC 7748 / RFC 8032 / RFC 5869 implementations.
 * All randomness comes from [SecureRandom], which on Android is the platform CSPRNG.
 */
class TinkHandshakeCrypto(private val random: SecureRandom = SecureRandom()) : HandshakeCrypto {

    override fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    override fun generateX25519KeyPair(): X25519KeyPair {
        val privateKey = randomBytes(ProtocolConstants.X25519_KEY_SIZE)
        clampX25519(privateKey)
        return X25519KeyPair(privateKey, X25519.publicFromPrivate(privateKey))
    }

    override fun x25519(privateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        if (peerPublicKey.size != ProtocolConstants.X25519_KEY_SIZE) {
            throw InvalidPeerKeyException("X25519 public key must be 32 bytes, got ${peerPublicKey.size}")
        }
        val secret = try {
            X25519.computeSharedSecret(privateKey, peerPublicKey)
        } catch (e: InvalidKeyException) {
            throw InvalidPeerKeyException("X25519 public key rejected", e)
        }
        if (secret.isAllZero()) {
            secret.wipe()
            throw InvalidPeerKeyException("X25519 produced an all-zero shared secret")
        }
        return secret
    }

    override fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray =
        Hkdf.computeHkdf("HmacSHA256", ikm, salt, info, length)

    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    override fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    override fun verifyEd25519(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (publicKey.size != ProtocolConstants.ED25519_PUBLIC_KEY_SIZE ||
            signature.size != ProtocolConstants.ED25519_SIGNATURE_SIZE
        ) return false
        return try {
            Ed25519Verify(publicKey).verify(signature, message)
            true
        } catch (e: GeneralSecurityException) {
            false
        }
    }

    companion object {
        /** RFC 7748 scalar clamping; X25519 implementations also clamp internally. */
        fun clampX25519(privateKey: ByteArray) {
            privateKey[0] = (privateKey[0].toInt() and 248).toByte()
            privateKey[31] = ((privateKey[31].toInt() and 127) or 64).toByte()
        }
    }
}

/** Ed25519 signer holding a raw 32-byte seed. Used by tests, test vectors and the reference responder. */
class Ed25519SeedSigner(seed: ByteArray) : IdentitySigner {
    private val signer = Ed25519Sign(seed)
    val publicKey: ByteArray = Ed25519Sign.KeyPair.newKeyPairFromSeed(seed).publicKey

    override suspend fun sign(message: ByteArray): ByteArray = signer.sign(message)
}

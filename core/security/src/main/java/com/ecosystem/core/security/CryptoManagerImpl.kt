package com.ecosystem.core.security

import android.util.Base64
import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import com.google.crypto.tink.subtle.X25519
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManagerImpl @Inject constructor(
    private val secureStorage: SecureStorage
) : CryptoManager {

    override fun generateIdentityKeyPair(alias: String): String {
        val keyPair = Ed25519Sign.KeyPair.newKeyPair()
        val privateKeyBase64 = Base64.encodeToString(keyPair.privateKey, Base64.NO_WRAP)
        val publicKeyBase64 = Base64.encodeToString(keyPair.publicKey, Base64.NO_WRAP)

        // SecureStorage encrypts values using Keystore AES-GCM before saving to SharedPreferences
        secureStorage.putString("${alias}_prv", privateKeyBase64)
        secureStorage.putString("${alias}_pub", publicKeyBase64)

        return publicKeyBase64
    }

    override fun signWithIdentityKey(alias: String, data: ByteArray): ByteArray {
        val privateKeyBase64 = secureStorage.getString("${alias}_prv")
            ?: throw IllegalStateException("Identity key pair for alias '$alias' has not been generated.")
        val privateKeyBytes = Base64.decode(privateKeyBase64, Base64.NO_WRAP)
        val signer = Ed25519Sign(privateKeyBytes)
        return signer.sign(data)
    }

    override fun verifySignature(publicKeyBase64: String, data: ByteArray, signature: ByteArray): Boolean {
        return try {
            val publicKeyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val verifier = Ed25519Verify(publicKeyBytes)
            verifier.verify(signature, data)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getIdentityPublicKey(alias: String): String? {
        return secureStorage.getString("${alias}_pub")
    }

    override fun generateEphemeralKeyPair(): Pair<ByteArray, String> {
        val privateKeyRaw = X25519.generatePrivateKey()
        val publicKeyRaw = X25519.publicFromPrivate(privateKeyRaw)
        val publicKeyBase64 = Base64.encodeToString(publicKeyRaw, Base64.NO_WRAP)
        return Pair(privateKeyRaw, publicKeyBase64)
    }

    override fun computeSharedSessionKey(localPrivateKey: ByteArray, peerPublicKeyBase64: String): ByteArray {
        val peerPublicKeyRaw = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val sharedSecret = X25519.computeSharedSecret(localPrivateKey, peerPublicKeyRaw)

        // Deriving AES-256 session key using HKDF-SHA256 (Tink subtle helper)
        return com.google.crypto.tink.subtle.Hkdf.computeHkdf(
            "HmacSHA256",
            sharedSecret,
            ByteArray(0),
            ByteArray(0),
            32
        )
    }

    override fun computeHmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKeySpec = javax.crypto.spec.SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKeySpec)
        return mac.doFinal(data)
    }
}

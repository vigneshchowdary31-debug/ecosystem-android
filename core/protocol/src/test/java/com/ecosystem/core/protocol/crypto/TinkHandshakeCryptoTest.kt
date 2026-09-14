package com.ecosystem.core.protocol.crypto

import com.ecosystem.core.protocol.util.hexToBytes
import com.ecosystem.core.protocol.util.toHex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Known-answer tests pinning the primitives to their RFCs, so both platforms can be checked against the same vectors. */
class TinkHandshakeCryptoTest {

    private val crypto = TinkHandshakeCrypto()

    @Test
    fun `hkdf sha256 matches RFC 5869 test case 1`() {
        val okm = crypto.hkdfSha256(
            ikm = hexToBytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
            salt = hexToBytes("000102030405060708090a0b0c"),
            info = hexToBytes("f0f1f2f3f4f5f6f7f8f9"),
            length = 42,
        )
        assertEquals(
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
            okm.toHex(),
        )
    }

    @Test
    fun `hkdf sha256 with empty salt and info matches RFC 5869 test case 3`() {
        val okm = crypto.hkdfSha256(
            ikm = hexToBytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
            salt = ByteArray(0),
            info = ByteArray(0),
            length = 42,
        )
        assertEquals(
            "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8",
            okm.toHex(),
        )
    }

    @Test
    fun `x25519 matches RFC 7748 section 6_1`() {
        val alicePrivate = hexToBytes("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
        val bobPublic = hexToBytes("de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f")
        assertEquals(
            "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742",
            crypto.x25519(alicePrivate, bobPublic).toHex(),
        )
    }

    @Test
    fun `x25519 rejects wrong-length and all-zero peer keys`() {
        val pair = crypto.generateX25519KeyPair()
        assertThrows<InvalidPeerKeyException> { crypto.x25519(pair.privateKey, ByteArray(31)) }
        assertThrows<InvalidPeerKeyException> { crypto.x25519(pair.privateKey, ByteArray(32)) }
    }

    @Test
    fun `ed25519 matches RFC 8032 test 1 and rejects tampering`() = runBlocking {
        val signer = Ed25519SeedSigner(hexToBytes("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"))
        assertEquals("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a", signer.publicKey.toHex())
        val signature = signer.sign(ByteArray(0))
        assertEquals(
            "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
            signature.toHex(),
        )
        assertTrue(crypto.verifyEd25519(signer.publicKey, ByteArray(0), signature))
        assertFalse(crypto.verifyEd25519(signer.publicKey, byteArrayOf(1), signature))
        signature[0] = (signature[0].toInt() xor 1).toByte()
        assertFalse(crypto.verifyEd25519(signer.publicKey, ByteArray(0), signature))
        assertFalse(crypto.verifyEd25519(ByteArray(31), ByteArray(0), signature))
    }

    @Test
    fun `hmac sha256 matches RFC 4231 test case 2`() {
        val mac = crypto.hmacSha256("Jefe".toByteArray(), "what do ya want for nothing?".toByteArray())
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", mac.toHex())
    }

    @Test
    fun `random bytes and ephemeral keys are fresh`() {
        assertNotEquals(crypto.randomBytes(32).toHex(), crypto.randomBytes(32).toHex())
        assertNotEquals(crypto.generateX25519KeyPair().publicKey.toHex(), crypto.generateX25519KeyPair().publicKey.toHex())
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            assertTrue("Expected ${T::class.simpleName} but got $e", e is T)
            return
        }
        throw AssertionError("Expected ${T::class.simpleName}")
    }
}

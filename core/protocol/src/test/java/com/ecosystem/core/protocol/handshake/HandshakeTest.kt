package com.ecosystem.core.protocol.handshake

import com.ecosystem.core.protocol.crypto.Ed25519SeedSigner
import com.ecosystem.core.protocol.crypto.TinkHandshakeCrypto
import com.ecosystem.core.protocol.framing.Frame
import com.ecosystem.core.protocol.testing.InMemoryEndpoint
import com.ecosystem.core.protocol.testing.InMemoryLink
import com.ecosystem.core.protocol.testing.ResponderHandshake
import com.ecosystem.core.protocol.testing.ResponderResult
import com.ecosystem.core.protocol.util.toHex
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class HandshakeTest {

    private val crypto = TinkHandshakeCrypto()
    private val androidSigner = Ed25519SeedSigner(ByteArray(32) { 0x11 })
    private val macSigner = Ed25519SeedSigner(ByteArray(32) { 0x22 })
    private val attackerSigner = Ed25519SeedSigner(ByteArray(32) { 0x33 })
    private val androidId = UUID.fromString("5f1c2b7a-3d4e-4f60-8a9b-0c1d2e3f4a5b")
    private val macId = UUID.fromString("2d861d8a-6b83-4a1d-8f9c-76e48c3f8f11")

    private val android = LocalPairingIdentity(androidId, androidSigner.publicKey, "Pixel 9 Pro", androidSigner)
    private val mac = LocalPairingIdentity(macId, macSigner.publicKey, "Studio Mac", macSigner)
    private val expectedMac = ExpectedPeer(macId, macSigner.publicKey, "Studio Mac")
    private val androidIp = byteArrayOf(192.toByte(), 168.toByte(), 1, 23)
    private val macIp = byteArrayOf(192.toByte(), 168.toByte(), 1, 10)

    private class Outcome(val android: Result<HandshakeResult>, val mac: Result<ResponderResult>)

    private suspend fun runHandshake(
        responderIdentity: LocalPairingIdentity = mac,
        expected: ExpectedPeer = expectedMac,
        link: Pair<InMemoryEndpoint, InMemoryEndpoint> = InMemoryLink.pair(),
        initiator: InitiatorHandshake = InitiatorHandshake(crypto, android, expected, androidIp),
        attacker: Boolean = false,
    ): Outcome = coroutineScope {
        val (androidEnd, macEnd) = link
        val responder = ResponderHandshake(crypto, responderIdentity, macIp, verifyAndroidAuthentication = !attacker)
        val macResult = async { runCatching { responder.run(macEnd) } }
        val androidResult = runCatching { initiator.run(androidEnd) }
        if (androidResult.isFailure) androidEnd.disconnect()
        Outcome(androidResult, macResult.await())
    }

    private fun Outcome.androidFailure(): HandshakeException =
        android.exceptionOrNull() as? HandshakeException
            ?: throw AssertionError("Expected a HandshakeException but got ${android.exceptionOrNull() ?: android.getOrNull()}")

    private fun Frame.rewrite(transform: (PairingMessage) -> PairingMessage): Frame =
        transform(PairingMessageCodec.decode(this)).toFrame()

    @Test
    fun `valid transcript completes on both sides`() = runTest {
        val outcome = runHandshake()
        val androidResult = outcome.android.getOrThrow()
        val macResult = outcome.mac.getOrThrow()

        assertEquals(macId, androidResult.peerDeviceId)
        assertArrayEquals(macSigner.publicKey, androidResult.peerIdentityPublicKey)
        assertArrayEquals(macIp, androidResult.peerIpAddress)
        assertEquals(androidId, macResult.androidDeviceId)
        assertArrayEquals(androidSigner.publicKey, macResult.androidIdentityPublicKey)
        assertEquals("Pixel 9 Pro", macResult.androidDeviceName)
        assertArrayEquals(androidIp, macResult.androidIpAddress)
        assertArrayEquals(androidResult.transcriptHash, macResult.transcriptHash)
    }

    @Test
    fun `every packet respects the default ATT payload when fragmented`() = runTest {
        val link = InMemoryLink.pair(chunkSize = 20)
        runHandshake(link = link).android.getOrThrow()
        assertTrue(link.first.sentPacketSizes.all { it <= 20 })
        assertTrue(link.second.sentPacketSizes.all { it <= 20 })
        assertTrue("M3 must have been split", link.first.sentPacketSizes.size > 10)
    }

    @Test
    fun `android sends its ephemeral key first without waiting for the mac`() = runTest {
        val link = InMemoryLink.pair()
        runHandshake(link = link).android.getOrThrow()
        assertEquals(MessageType.KEY_EXCHANGE_INIT.code, link.first.sentFrames.first().type)
        assertEquals(
            listOf(0x01, 0x03, 0x05),
            link.first.sentFrames.map { it.type },
        )
        assertEquals(listOf(0x02, 0x04, 0x06), link.second.sentFrames.map { it.type })
    }

    @Test
    fun `wrong identity key is rejected by android`() = runTest {
        val impostor = LocalPairingIdentity(macId, attackerSigner.publicKey, "Studio Mac", attackerSigner)
        val outcome = runHandshake(responderIdentity = impostor, attacker = true)
        assertEquals(HandshakeException.Reason.INVALID_SIGNATURE, outcome.androidFailure().reason)
    }

    @Test
    fun `an honest mac with a different identity also stops because the transcript binds both keys`() = runTest {
        val otherMac = LocalPairingIdentity(macId, attackerSigner.publicKey, "Other Mac", attackerSigner)
        val outcome = runHandshake(responderIdentity = otherMac)
        assertTrue(outcome.mac.isFailure)
        val failure = outcome.androidFailure()
        assertEquals(HandshakeException.Reason.PEER_REJECTED, failure.reason)
        assertEquals(ErrorCode.AUTHENTICATION_FAILED.code, failure.peerErrorCode)
    }

    @Test
    fun `wrong device id is rejected`() = runTest {
        val otherId = LocalPairingIdentity(UUID.randomUUID(), macSigner.publicKey, "Studio Mac", macSigner)
        val outcome = runHandshake(responderIdentity = otherId, attacker = true)
        assertEquals(HandshakeException.Reason.WRONG_DEVICE_ID, outcome.androidFailure().reason)
    }

    @Test
    fun `substituted mac ephemeral key is rejected`() = runTest {
        val link = InMemoryLink.pair()
        val attackerKey = crypto.generateX25519KeyPair().publicKey
        link.second.frameInterceptor = { frame ->
            if (frame.type == MessageType.KEY_EXCHANGE_RESPONSE.code) {
                frame.rewrite { KeyExchangeResponse(attackerKey, (it as KeyExchangeResponse).nonce) }
            } else frame
        }
        val outcome = runHandshake(link = link, attacker = true)
        assertEquals(HandshakeException.Reason.INVALID_SIGNATURE, outcome.androidFailure().reason)
    }

    @Test
    fun `substituted android ephemeral key is rejected by the mac`() = runTest {
        val link = InMemoryLink.pair()
        val attackerKey = crypto.generateX25519KeyPair().publicKey
        link.first.frameInterceptor = { frame ->
            if (frame.type == MessageType.KEY_EXCHANGE_INIT.code) {
                frame.rewrite { KeyExchangeInit(attackerKey, (it as KeyExchangeInit).nonce) }
            } else frame
        }
        val outcome = runHandshake(link = link)
        assertTrue(outcome.mac.isFailure)
        assertTrue(outcome.android.isFailure)
    }

    @Test
    fun `tampered challenge is rejected`() = runTest {
        val link = InMemoryLink.pair()
        link.second.frameInterceptor = { frame ->
            if (frame.type == MessageType.KEY_EXCHANGE_RESPONSE.code) {
                frame.rewrite {
                    val response = it as KeyExchangeResponse
                    val nonce = response.nonce.copyOf().also { n -> n[0] = (n[0].toInt() xor 0x01).toByte() }
                    KeyExchangeResponse(response.ephemeralPublicKey, nonce)
                }
            } else frame
        }
        assertEquals(HandshakeException.Reason.INVALID_SIGNATURE, runHandshake(link = link, attacker = true).androidFailure().reason)
    }

    @Test
    fun `tampered signature is rejected`() = runTest {
        val link = InMemoryLink.pair()
        link.second.frameInterceptor = { frame ->
            if (frame.type == MessageType.MAC_AUTH.code) {
                frame.rewrite {
                    val auth = it as MacAuth
                    val signature = auth.signature.copyOf().also { s -> s[5] = (s[5].toInt() xor 0x40).toByte() }
                    MacAuth(auth.deviceId, auth.ipAddress, signature, auth.authMac)
                }
            } else frame
        }
        assertEquals(HandshakeException.Reason.INVALID_SIGNATURE, runHandshake(link = link).androidFailure().reason)
    }

    @Test
    fun `tampered hmac is rejected`() = runTest {
        val link = InMemoryLink.pair()
        link.second.frameInterceptor = { frame ->
            if (frame.type == MessageType.MAC_AUTH.code) {
                frame.rewrite {
                    val auth = it as MacAuth
                    MacAuth(auth.deviceId, auth.ipAddress, auth.signature, ByteArray(32) { 7 })
                }
            } else frame
        }
        assertEquals(HandshakeException.Reason.INVALID_MAC, runHandshake(link = link).androidFailure().reason)
    }

    @Test
    fun `tampered mac ip address invalidates the signature`() = runTest {
        val link = InMemoryLink.pair()
        link.second.frameInterceptor = { frame ->
            if (frame.type == MessageType.MAC_AUTH.code) {
                frame.rewrite {
                    val auth = it as MacAuth
                    MacAuth(auth.deviceId, byteArrayOf(10, 0, 0, 66), auth.signature, auth.authMac)
                }
            } else frame
        }
        assertEquals(HandshakeException.Reason.INVALID_SIGNATURE, runHandshake(link = link).androidFailure().reason)
    }

    @Test
    fun `tampered completion mac is rejected`() = runTest {
        val link = InMemoryLink.pair()
        link.second.frameInterceptor = { frame ->
            if (frame.type == MessageType.MAC_COMPLETE.code) MacComplete(ByteArray(32)).toFrame() else frame
        }
        assertEquals(HandshakeException.Reason.INVALID_MAC, runHandshake(link = link).androidFailure().reason)
    }

    @Test
    fun `mac rejects a tampered android signature and android learns why`() = runTest {
        val link = InMemoryLink.pair()
        link.first.frameInterceptor = { frame ->
            if (frame.type == MessageType.ANDROID_AUTH.code) {
                frame.rewrite {
                    val auth = it as AndroidAuth
                    val signature = auth.signature.copyOf().also { s -> s[0] = (s[0].toInt() xor 1).toByte() }
                    AndroidAuth(auth.deviceId, auth.identityPublicKey, auth.deviceNameBytes, auth.ipAddress, signature, auth.authMac)
                }
            } else frame
        }
        val outcome = runHandshake(link = link)
        assertTrue(outcome.mac.isFailure)
        val failure = outcome.androidFailure()
        assertEquals(HandshakeException.Reason.PEER_REJECTED, failure.reason)
        assertEquals(ErrorCode.AUTHENTICATION_FAILED.code, failure.peerErrorCode)
    }

    @Test
    fun `replayed mac messages from an earlier session are rejected`() = runTest {
        val firstLink = InMemoryLink.pair()
        runHandshake(link = firstLink).android.getOrThrow()
        val recorded = firstLink.second.sentFrames.toList()

        val (androidEnd, fakeMacEnd) = InMemoryLink.pair()
        coroutineScope {
            launch {
                runCatching {
                    fakeMacEnd.receive(10_000)
                    fakeMacEnd.send(recorded[0])
                    fakeMacEnd.receive(10_000)
                    fakeMacEnd.send(recorded[1])
                    fakeMacEnd.receive(10_000)
                    fakeMacEnd.send(recorded[2])
                }
            }
            val result = runCatching { InitiatorHandshake(crypto, android, expectedMac).run(androidEnd) }
            androidEnd.disconnect()
            val failure = result.exceptionOrNull() as HandshakeException
            assertEquals(HandshakeException.Reason.INVALID_SIGNATURE, failure.reason)
        }
    }

    @Test
    fun `reflected key exchange is rejected`() = runTest {
        val (androidEnd, macEnd) = InMemoryLink.pair()
        coroutineScope {
            launch {
                runCatching {
                    val init = PairingMessageCodec.decode(macEnd.receive(10_000)) as KeyExchangeInit
                    macEnd.send(KeyExchangeResponse(init.ephemeralPublicKey, init.nonce).toFrame())
                }
            }
            val failure = runCatching { InitiatorHandshake(crypto, android, expectedMac).run(androidEnd) }
                .exceptionOrNull() as HandshakeException
            androidEnd.disconnect()
            assertEquals(HandshakeException.Reason.REFLECTED_MESSAGE, failure.reason)
        }
    }

    @Test
    fun `low order mac ephemeral key is rejected`() = runTest {
        val (androidEnd, macEnd) = InMemoryLink.pair()
        coroutineScope {
            launch {
                runCatching {
                    macEnd.receive(10_000)
                    macEnd.send(KeyExchangeResponse(ByteArray(32), ByteArray(32) { 9 }).toFrame())
                }
            }
            val failure = runCatching { InitiatorHandshake(crypto, android, expectedMac).run(androidEnd) }
                .exceptionOrNull() as HandshakeException
            androidEnd.disconnect()
            assertEquals(HandshakeException.Reason.INVALID_PEER_KEY, failure.reason)
        }
    }

    @Test
    fun `mismatched protocol version in the frame header is rejected`() = runTest {
        val link = InMemoryLink.pair()
        link.second.byteInterceptor = { bytes -> bytes.copyOf().also { it[2] = 0x03 } }
        assertEquals(HandshakeException.Reason.PROTOCOL_VERSION_MISMATCH, runHandshake(link = link).androidFailure().reason)
    }

    @Test
    fun `peer reporting unsupported version is a version mismatch`() = runTest {
        val (androidEnd, macEnd) = InMemoryLink.pair()
        coroutineScope {
            launch {
                runCatching {
                    macEnd.receive(10_000)
                    macEnd.send(ErrorMessage(ErrorCode.UNSUPPORTED_VERSION).toFrame())
                }
            }
            val failure = runCatching { InitiatorHandshake(crypto, android, expectedMac).run(androidEnd) }
                .exceptionOrNull() as HandshakeException
            androidEnd.disconnect()
            assertEquals(HandshakeException.Reason.PROTOCOL_VERSION_MISMATCH, failure.reason)
        }
    }

    @Test
    fun `malformed payload is rejected`() = runTest {
        val (androidEnd, macEnd) = InMemoryLink.pair()
        coroutineScope {
            launch {
                runCatching {
                    macEnd.receive(10_000)
                    macEnd.send(Frame(MessageType.KEY_EXCHANGE_RESPONSE.code, byteArrayOf(0x20, 0x00, 0x01, 0x00)))
                }
            }
            val failure = runCatching { InitiatorHandshake(crypto, android, expectedMac).run(androidEnd) }
                .exceptionOrNull() as HandshakeException
            androidEnd.disconnect()
            assertEquals(HandshakeException.Reason.MALFORMED_MESSAGE, failure.reason)
        }
    }

    @Test
    fun `out of order message is rejected`() = runTest {
        val (androidEnd, macEnd) = InMemoryLink.pair()
        coroutineScope {
            launch {
                runCatching {
                    macEnd.receive(10_000)
                    macEnd.send(MacComplete(ByteArray(32)).toFrame())
                }
            }
            val failure = runCatching { InitiatorHandshake(crypto, android, expectedMac).run(androidEnd) }
                .exceptionOrNull() as HandshakeException
            androidEnd.disconnect()
            assertEquals(HandshakeException.Reason.UNEXPECTED_MESSAGE, failure.reason)
        }
    }

    @Test
    fun `silent peer times out`() = runTest {
        val (androidEnd, _) = InMemoryLink.pair()
        val failure = runCatching { InitiatorHandshake(crypto, android, expectedMac).run(androidEnd) }
            .exceptionOrNull() as HandshakeException
        assertEquals(HandshakeException.Reason.TIMEOUT, failure.reason)
    }

    @Test
    fun `disconnect during the handshake is reported`() = runTest {
        val (androidEnd, macEnd) = InMemoryLink.pair()
        coroutineScope {
            launch {
                macEnd.receive(10_000)
                macEnd.disconnect()
            }
            val failure = runCatching { InitiatorHandshake(crypto, android, expectedMac).run(androidEnd) }
                .exceptionOrNull() as HandshakeException
            assertEquals(HandshakeException.Reason.DISCONNECTED, failure.reason)
        }
    }

    @Test
    fun `secrets are wiped after success and after failure`() = runTest {
        var heldDuringHandshake = false
        lateinit var success: InitiatorHandshake
        success = InitiatorHandshake(crypto, android, expectedMac, listener = object : InitiatorHandshake.Listener {
            override fun onKeyExchangeComplete() {
                heldDuringHandshake = success.holdsSecrets
            }
        })
        runHandshake(initiator = success).android.getOrThrow()
        assertTrue(heldDuringHandshake)
        assertFalse(success.holdsSecrets)

        val impostor = LocalPairingIdentity(macId, attackerSigner.publicKey, "Studio Mac", attackerSigner)
        val failing = InitiatorHandshake(crypto, android, expectedMac)
        runHandshake(responderIdentity = impostor, initiator = failing)
        assertFalse(failing.holdsSecrets)
    }

    @Test
    fun `a handshake instance cannot be reused`() = runTest {
        val initiator = InitiatorHandshake(crypto, android, expectedMac)
        runHandshake(initiator = initiator).android.getOrThrow()
        val failure = runCatching { initiator.run(InMemoryLink.pair().first) }.exceptionOrNull() as HandshakeException
        assertEquals(HandshakeException.Reason.ALREADY_USED, failure.reason)
    }

    @Test
    fun `concurrent sessions never share key material`() = runTest {
        val first = InitiatorHandshake(crypto, android, expectedMac)
        val second = InitiatorHandshake(crypto, android, expectedMac)
        val results = coroutineScope {
            val a = async { runHandshake(initiator = first) }
            val b = async { runHandshake(initiator = second) }
            listOf(a.await(), b.await())
        }
        val hashes = results.map { it.android.getOrThrow().transcriptHash.toHex() }
        assertNotEquals(hashes[0], hashes[1])
        assertFalse(first.holdsSecrets)
        assertFalse(second.holdsSecrets)
    }
}

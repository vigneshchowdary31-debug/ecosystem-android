package com.ecosystem.core.protocol.qr

import com.ecosystem.core.protocol.util.Base64Codec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class QrPayloadCodecTest {

    private val deviceId = UUID.fromString("2d861d8a-6b83-4a1d-8f9c-76e48c3f8f11")
    private val advertisingId = UUID.fromString("f7823ab4-3e9a-4c28-be51-24b893a201f9")
    private val publicKey = ByteArray(32) { (it * 7 + 3).toByte() }
    private val pk = Base64Codec.encodeUrlNoPadding(publicKey)

    private fun qr(
        v: String = "2",
        id: String = deviceId.toString(),
        name: String = "MacBook%20Pro",
        adv: String = advertisingId.toString(),
        key: String = pk,
    ) = "connectflow://pair?v=$v&id=$id&name=$name&adv=$adv&pk=$key"

    private fun parseError(text: String): QrError {
        val result = QrPayloadCodec.parse(text)
        assertTrue("Expected Invalid for $text", result is QrParseResult.Invalid)
        return (result as QrParseResult.Invalid).error
    }

    private fun parseValid(text: String): QrPairingPayload {
        val result = QrPayloadCodec.parse(text)
        assertTrue("Expected Valid but got ${(result as? QrParseResult.Invalid)?.detail}", result is QrParseResult.Valid)
        return (result as QrParseResult.Valid).payload
    }

    @Test
    fun `valid code parses every field`() {
        val payload = parseValid(qr())
        assertEquals(2, payload.version)
        assertEquals(deviceId, payload.deviceId)
        assertEquals("MacBook Pro", payload.deviceName)
        assertEquals(advertisingId, payload.advertisingIdentifier)
        assertArrayEquals(publicKey, payload.identityPublicKey)
    }

    @Test
    fun `encode then parse round-trips names with delimiters and unicode`() {
        val name = "Vignesh’s MacBook Pro, 14\" & more=+"
        val original = QrPairingPayload(2, deviceId, name, advertisingId, publicKey)
        val parsed = parseValid(QrPayloadCodec.encode(original))
        assertEquals(name, parsed.deviceName)
        assertEquals(deviceId, parsed.deviceId)
        assertArrayEquals(publicKey, parsed.identityPublicKey)
    }

    @Test
    fun `upper-case UUIDs from macOS are accepted`() {
        val payload = parseValid(qr(id = deviceId.toString().uppercase(), adv = advertisingId.toString().uppercase()))
        assertEquals(deviceId, payload.deviceId)
    }

    @Test
    fun `fields may appear in any order`() {
        parseValid("connectflow://pair?pk=$pk&adv=$advertisingId&name=Mac&id=$deviceId&v=2")
    }

    @Test
    fun `invalid prefix is not a pairing code`() {
        assertEquals(QrError.NOT_A_PAIRING_CODE, parseError("https://example.com/?v=2"))
        assertEquals(QrError.NOT_A_PAIRING_CODE, parseError("CONNECTFLOW://pair?v=2"))
        assertEquals(QrError.NOT_A_PAIRING_CODE, parseError(""))
    }

    @Test
    fun `legacy v1 csv code is reported as legacy`() {
        assertEquals(QrError.LEGACY_FORMAT, parseError("identity_continuity:$deviceId,MacBook,$advertisingId,abc="))
    }

    @Test
    fun `wrong UUIDs are rejected`() {
        assertEquals(QrError.INVALID_DEVICE_ID, parseError(qr(id = "not-a-uuid")))
        assertEquals(QrError.INVALID_DEVICE_ID, parseError(qr(id = "1-1-1-1-1")))
        assertEquals(QrError.INVALID_DEVICE_ID, parseError(qr(id = deviceId.toString().replace("-", ""))))
        assertEquals(QrError.INVALID_ADVERTISING_ID, parseError(qr(adv = "zzzzzzzz-3e9a-4c28-be51-24b893a201f9")))
    }

    @Test
    fun `invalid base64 is rejected`() {
        assertEquals(QrError.INVALID_PUBLIC_KEY, parseError(qr(key = pk.dropLast(1) + "*")))
        // Standard (not URL-safe) alphabet characters are rejected.
        assertEquals(QrError.INVALID_PUBLIC_KEY, parseError(qr(key = "/" + pk.drop(1))))
        assertEquals(QrError.INVALID_PUBLIC_KEY, parseError(qr(key = "+" + pk.drop(1))))
    }

    @Test
    fun `non canonical base64 trailing bits are rejected`() {
        // The 43rd character carries 2 unused bits; setting them must not decode to the same key.
        val last = pk.last()
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val tweaked = alphabet[(alphabet.indexOf(last) or 1)]
        if (tweaked != last) assertEquals(QrError.INVALID_PUBLIC_KEY, parseError(qr(key = pk.dropLast(1) + tweaked)))
    }

    @Test
    fun `wrong public key length is rejected`() {
        val short = Base64Codec.encodeUrlNoPadding(ByteArray(31) { 1 })
        val long = Base64Codec.encodeUrlNoPadding(ByteArray(33) { 1 })
        assertEquals(QrError.INVALID_PUBLIC_KEY, parseError(qr(key = short)))
        assertEquals(QrError.INVALID_PUBLIC_KEY, parseError(qr(key = long)))
    }

    @Test
    fun `malformed and unsupported versions are rejected`() {
        assertEquals(QrError.MALFORMED, parseError(qr(v = "02")))
        assertEquals(QrError.MALFORMED, parseError(qr(v = "two")))
        assertEquals(QrError.MALFORMED, parseError(qr(v = "")))
        assertEquals(QrError.UNSUPPORTED_VERSION, parseError(qr(v = "1")))
        assertEquals(QrError.UNSUPPORTED_VERSION, parseError(qr(v = "3")))
    }

    @Test
    fun `future versions are reported as unsupported even with new fields`() {
        assertEquals(QrError.UNSUPPORTED_VERSION, parseError(qr(v = "3") + "&token=abc"))
    }

    @Test
    fun `oversized input is rejected`() {
        val huge = qr(name = "a".repeat(600))
        assertEquals(QrError.TOO_LONG, parseError(huge))
    }

    @Test
    fun `missing duplicate and unknown fields are rejected`() {
        assertEquals(QrError.MISSING_FIELD, parseError("connectflow://pair?v=2&id=$deviceId&name=Mac&adv=$advertisingId"))
        assertEquals(QrError.DUPLICATE_FIELD, parseError(qr() + "&id=$deviceId"))
        assertEquals(QrError.UNKNOWN_FIELD, parseError(qr() + "&mac=AA:BB:CC:DD:EE:FF"))
        assertEquals(QrError.MISSING_FIELD, parseError("connectflow://pair?id=$deviceId"))
    }

    @Test
    fun `malformed structure is rejected`() {
        assertEquals(QrError.MALFORMED, parseError("connectflow://pair?"))
        assertEquals(QrError.MALFORMED, parseError(qr() + "&"))
        assertEquals(QrError.MALFORMED, parseError(qr() + "&x"))
        assertEquals(QrError.MALFORMED, parseError(qr(name = "Mac=Book")))
        assertEquals(QrError.MALFORMED, parseError(qr(name = "Mac Book")))
        assertEquals(QrError.MALFORMED, parseError(qr(name = "Macé")))
        assertEquals(QrError.MALFORMED, parseError(qr() + "#fragment"))
    }

    @Test
    fun `bad device names are rejected`() {
        assertEquals(QrError.INVALID_DEVICE_NAME, parseError(qr(name = "")))
        assertEquals(QrError.INVALID_DEVICE_NAME, parseError(qr(name = "Mac%2")))
        assertEquals(QrError.INVALID_DEVICE_NAME, parseError(qr(name = "Mac%ZZ")))
        assertEquals(QrError.INVALID_DEVICE_NAME, parseError(qr(name = "Mac%0A")))
        assertEquals(QrError.INVALID_DEVICE_NAME, parseError(qr(name = "%C3%28")))
        assertEquals(QrError.INVALID_DEVICE_NAME, parseError(qr(name = "%20%20")))
        assertEquals(QrError.INVALID_DEVICE_NAME, parseError(qr(name = "a".repeat(129))))
    }

    @Test
    fun `payload never carries a BLE address or session secret`() {
        val encoded = QrPayloadCodec.encode(QrPairingPayload(2, deviceId, "Mac", advertisingId, publicKey))
        val keys = encoded.substringAfter('?').split('&').map { it.substringBefore('=') }
        assertEquals(listOf("v", "id", "name", "adv", "pk"), keys)
    }
}

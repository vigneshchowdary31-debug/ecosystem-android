package com.ecosystem.core.pairing

import android.util.Base64
import com.ecosystem.core.identity.IdentityManager
import com.ecosystem.core.networking.BleChannel
import com.ecosystem.core.security.CryptoManager
import com.ecosystem.core.trusteddevices.TrustedDeviceEntity
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class PairingManagerImpl @Inject constructor(
    private val bleChannel: BleChannel,
    private val cryptoManager: CryptoManager,
    private val identityManager: IdentityManager,
    private val trustedDeviceRepository: TrustedDeviceRepository
) : PairingManager {

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    override val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    override suspend fun startPairing(qrPayload: String): Boolean {
        reset()
        _pairingState.value = PairingState.ParsingQr

        val qrData = parseQrPayload(qrPayload)
        if (qrData == null) {
            _pairingState.value = PairingState.Failed("Invalid QR code format. Expected 'identity_continuity:deviceId,name,mac,longTermPubKey,ephemeralPubKey'")
            return false
        }

        _pairingState.value = PairingState.ConnectingBle
        val isConnected = bleChannel.connect(qrData.bleMacAddress)
        if (!isConnected) {
            _pairingState.value = PairingState.Failed("Failed to establish BLE connection to ${qrData.name}")
            return false
        }

        try {
            _pairingState.value = PairingState.EphemeralKeyAgreement
            
            // 1. Generate local ephemeral X25519 keypair
            val (localEphemeralPrivate, localEphemeralPublicBase64) = cryptoManager.generateEphemeralKeyPair()

            // 2. Transmit local ephemeral public key to macOS
            val sendSuccess = bleChannel.send(localEphemeralPublicBase64.toByteArray(Charsets.UTF_8))
            if (!sendSuccess) {
                throw Exception("Failed to transmit ephemeral public key over BLE")
            }

            // 3. Await macOS ephemeral public key
            val peerEphemeralPublicKeyBytes = withTimeoutOrNull(8000) {
                bleChannel.incomingPackets.first()
            } ?: throw Exception("Timeout awaiting macOS ephemeral key response")

            val peerEphemeralPublicKeyBase64 = String(peerEphemeralPublicKeyBytes, Charsets.UTF_8)

            // 4. Derive AES shared key
            val sharedSessionKey = cryptoManager.computeSharedSessionKey(
                localEphemeralPrivate,
                peerEphemeralPublicKeyBase64
            )

            _pairingState.value = PairingState.SignatureVerification

            // 5. Generate and sign Android challenge
            val localChallenge = Random.nextBytes(16)
            val localIdentity = identityManager.getOrCreateIdentity()
            val localSignature = identityManager.signChallenge(localChallenge)

            val authPayload = "auth:" +
                    "${Base64.encodeToString(localChallenge, Base64.NO_WRAP)}:" +
                    "${Base64.encodeToString(localSignature, Base64.NO_WRAP)}:" +
                    localIdentity.publicKeyEd25519
            
            val sendAuthSuccess = bleChannel.send(authPayload.toByteArray(Charsets.UTF_8))
            if (!sendAuthSuccess) {
                throw Exception("Failed to transmit signature payload")
            }

            // 6. Await macOS authentication response
            val authRespBytes = withTimeoutOrNull(8000) {
                bleChannel.incomingPackets.first()
            } ?: throw Exception("Timeout awaiting macOS signature response")

            val authRespStr = String(authRespBytes, Charsets.UTF_8)
            if (!authRespStr.startsWith("auth_resp:")) {
                throw Exception("Malformed authentication packet received: $authRespStr")
            }

            // Format: auth_resp:macChallengeBase64:macSignatureBase64
            val parts = authRespStr.split(":")
            if (parts.size < 3) {
                throw Exception("Incomplete authentication payload from macOS")
            }

            val peerChallenge = Base64.decode(parts[1], Base64.NO_WRAP)
            val peerSignature = Base64.decode(parts[2], Base64.NO_WRAP)

            // 7. Verify macOS signature using long-term public key from QR code
            val signatureVerified = cryptoManager.verifySignature(
                qrData.publicKeyEd25519,
                peerChallenge,
                peerSignature
            )

            if (!signatureVerified) {
                throw Exception("Peer signature verification failed. Trust rejected.")
            }

            // 8. Key verified! Add to Room Trusted Devices
            val trustedDevice = TrustedDeviceEntity(
                deviceId = qrData.deviceId,
                name = qrData.name,
                bleMacAddress = qrData.bleMacAddress,
                lastKnownIpAddress = null,
                publicKeyEd25519 = qrData.publicKeyEd25519,
                isTrustActive = true,
                pairingTimestamp = System.currentTimeMillis(),
                lastSeenTimestamp = System.currentTimeMillis()
            )

            trustedDeviceRepository.addTrustedDevice(trustedDevice)
            _pairingState.value = PairingState.Success
            return true

        } catch (e: Exception) {
            _pairingState.value = PairingState.Failed(e.message ?: "An unexpected error occurred during pairing.")
            bleChannel.disconnect()
            return false
        }
    }

    override fun reset() {
        _pairingState.value = PairingState.Idle
    }

    private fun parseQrPayload(payload: String): QrPairingData? {
        // Expected format: identity_continuity:deviceId,name,bleMacAddress,publicKeyEd25519,ephemeralPublicKeyX25519
        if (!payload.startsWith("identity_continuity:")) return null
        val csv = payload.removePrefix("identity_continuity:")
        val parts = csv.split(",")
        if (parts.size < 5) return null

        return QrPairingData(
            deviceId = parts[0],
            name = parts[1],
            bleMacAddress = parts[2],
            publicKeyEd25519 = parts[3],
            ephemeralPublicKeyX25519 = parts[4]
        )
    }
}

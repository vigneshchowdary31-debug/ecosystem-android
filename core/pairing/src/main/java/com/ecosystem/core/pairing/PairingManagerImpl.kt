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
    private val trustedDeviceRepository: TrustedDeviceRepository,
    private val discoveryService: com.ecosystem.core.discovery.domain.repository.DiscoveryService
) : PairingManager {

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    override val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    override suspend fun startPairing(qrPayload: String): Boolean {
        reset()
        _pairingState.value = PairingState.ParsingQr

        val qrData = parseQrPayload(qrPayload)
        if (qrData == null) {
            _pairingState.value = PairingState.Failed("Invalid QR code format. Expected 'identity_continuity:deviceId,name,advertisingIdentifier,publicKeyEd25519'")
            return false
        }

        _pairingState.value = PairingState.ConnectingBle
        
        android.util.Log.d("PAIRING_DISCOVERY", "Starting scanning for candidates")
        discoveryService.startScanning(null)
        
        val pairingSuccess = withTimeoutOrNull(30000) {
            val attemptedMacs = mutableSetOf<String>()
            var matched = false
            try {
                discoveryService.scanResults.collect { results ->
                    val candidate = results.firstOrNull {
                        !attemptedMacs.contains(it.macAddress)
                    }
                    if (candidate != null) {
                        val mac = candidate.macAddress
                        attemptedMacs.add(mac)
                        
                        android.util.Log.d("PAIRING_DISCOVERY", "candidate found")
                        android.util.Log.d("PAIRING_DISCOVERY", "candidate MAC: $mac")
                        android.util.Log.d("PAIRING_DISCOVERY", "connecting")
                        
                        // Temporarily stop scanning to avoid interference during connection
                        discoveryService.stopScanning()
                        
                        val isConnected = bleChannel.connect(mac)
                        if (isConnected) {
                            android.util.Log.d("PAIRING_DISCOVERY", "connected")
                            try {
                                val verified = performHandshakeAndVerify(mac, qrData)
                                if (verified) {
                                    android.util.Log.d("PAIRING_DISCOVERY", "verification success")
                                    matched = true
                                    throw kotlinx.coroutines.CancellationException("Pairing successful")
                                } else {
                                    android.util.Log.w("PAIRING_DISCOVERY", "verification failure")
                                    bleChannel.disconnect()
                                    android.util.Log.w("PAIRING_DISCOVERY", "disconnect reason: Cryptographic verification failed")
                                    discoveryService.startScanning(null)
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                android.util.Log.w("PAIRING_DISCOVERY", "verification failure")
                                bleChannel.disconnect()
                                android.util.Log.w("PAIRING_DISCOVERY", "disconnect reason: ${e.message ?: "Cryptographic verification failed"}")
                                discoveryService.startScanning(null)
                            }
                        } else {
                            android.util.Log.w("PAIRING_DISCOVERY", "disconnect reason: Connection failed")
                            // Resume scanning if connection failed
                            discoveryService.startScanning(null)
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (matched) {
                    // Success path exits here
                } else {
                    throw e
                }
            }
            matched
        } ?: false
        
        discoveryService.stopScanning()
        
        if (!pairingSuccess) {
            _pairingState.value = PairingState.Failed("Failed to discover and connect to candidate device with verification.")
            return false
        }
        
        return true
    }

    private suspend fun performHandshakeAndVerify(
        targetMacAddress: String,
        qrData: QrPairingData
    ): Boolean {
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

        // Compute Session Proof using derived AES shared session key
        val localSessionProofBytes = cryptoManager.computeHmacSha256(sharedSessionKey, localChallenge)
        val localSessionProofBase64 = Base64.encodeToString(localSessionProofBytes, Base64.NO_WRAP)
        
        // Resolve Android local IP address to share it
        val localIpAddress = getLocalIpAddress()

        // Pack: auth:<androidChallengeBase64>:<androidSignatureBase64>:<androidPublicKeyEd25519>:<androidSessionProofBase64>:<androidIpAddress>
        val authPayload = "auth:" +
                "${Base64.encodeToString(localChallenge, Base64.NO_WRAP)}:" +
                "${Base64.encodeToString(localSignature, Base64.NO_WRAP)}:" +
                "${localIdentity.publicKeyEd25519}:" +
                "${localSessionProofBase64}:" +
                localIpAddress
        
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

        // Expected parts: auth_resp:<macChallengeBase64>:<macSignatureBase64>:<macSessionProofBase64>:<macIpAddress>
        val parts = authRespStr.split(":")
        if (parts.size < 5) {
            throw Exception("Incomplete authentication payload from macOS (expected 5 fields, received ${parts.size})")
        }

        val peerChallenge = Base64.decode(parts[1], Base64.NO_WRAP)
        val peerSignature = Base64.decode(parts[2], Base64.NO_WRAP)
        val peerSessionProof = Base64.decode(parts[3], Base64.NO_WRAP)
        val peerIpAddress = parts[4]

        // 7. Verify macOS signature using long-term public key from QR code
        val signatureVerified = cryptoManager.verifySignature(
            qrData.publicKeyEd25519,
            peerChallenge,
            peerSignature
        )

        if (!signatureVerified) {
            throw Exception("Peer signature verification failed. Trust rejected.")
        }

        // 8. Verify macOS session proof
        val expectedPeerSessionProof = cryptoManager.computeHmacSha256(sharedSessionKey, peerChallenge)
        if (!peerSessionProof.contentEquals(expectedPeerSessionProof)) {
            throw Exception("Peer session proof validation failed. Trust rejected.")
        }

        // 9. Key verified! Add to Room Trusted Devices
        val trustedDevice = TrustedDeviceEntity(
            deviceId = qrData.deviceId,
            name = qrData.name,
            bleMacAddress = targetMacAddress,
            lastKnownIpAddress = if (peerIpAddress == "0.0.0.0" || peerIpAddress == "unknown") null else peerIpAddress,
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
        // Expected format: identity_continuity:deviceId,name,advertisingIdentifier,publicKeyEd25519
        if (!payload.startsWith("identity_continuity:")) return null
        val csv = payload.removePrefix("identity_continuity:")
        val parts = csv.split(",")
        if (parts.size < 4) return null

        return QrPairingData(
            deviceId = parts[0],
            name = parts[1],
            advertisingIdentifier = parts[2],
            publicKeyEd25519 = parts[3]
        )
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (ex: Exception) {
            // Ignore
        }
        return "0.0.0.0"
    }
}

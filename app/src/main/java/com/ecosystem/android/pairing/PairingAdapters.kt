package com.ecosystem.android.pairing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ecosystem.core.common.log.AppLog
import com.ecosystem.core.identity.IdentityManager
import com.ecosystem.core.pairing.IdentityUnavailableException
import com.ecosystem.core.pairing.LocalAddressProvider
import com.ecosystem.core.pairing.LocalIdentitySource
import com.ecosystem.core.pairing.PairedPeer
import com.ecosystem.core.pairing.PairingLogger
import com.ecosystem.core.pairing.TrustedPeerStore
import com.ecosystem.core.protocol.handshake.LocalPairingIdentity
import com.ecosystem.core.protocol.util.Base64Codec
import com.ecosystem.core.protocol.util.Uuids
import com.ecosystem.core.security.IdentityKeyUnavailableException
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import javax.inject.Inject

/** Exposes the device identity to the handshake. The private key never leaves IdentityManager. */
class IdentityPairingSource @Inject constructor(
    private val identityManager: IdentityManager,
) : LocalIdentitySource {

    override suspend fun loadIdentity(): LocalPairingIdentity {
        val identity = try {
            identityManager.getOrCreateIdentity()
        } catch (e: IdentityKeyUnavailableException) {
            throw IdentityUnavailableException("Device identity cannot be read", e)
        }
        val deviceId = Uuids.parseCanonical(identity.deviceId)
            ?: throw IdentityUnavailableException("Stored device ID is not a UUID")
        val publicKey = Base64Codec.decodeStandard(identity.publicKeyEd25519)?.takeIf { it.size == 32 }
            ?: throw IdentityUnavailableException("Stored identity public key is invalid")
        return LocalPairingIdentity(deviceId, publicKey, identity.name) { message ->
            try {
                identityManager.sign(message)
            } catch (e: IdentityKeyUnavailableException) {
                throw IdentityUnavailableException("Identity key unavailable for signing", e)
            }
        }
    }
}

class RoomTrustedPeerStore @Inject constructor(
    private val repository: TrustedDeviceRepository,
) : TrustedPeerStore {

    override suspend fun savePairedPeer(peer: PairedPeer) {
        repository.savePairedDevice(
            deviceId = Uuids.canonical(peer.deviceId),
            name = peer.name,
            publicKey = peer.identityPublicKey,
            ipAddress = peer.ipAddress,
            timestamp = peer.pairedAtMillis,
        )
    }
}

/** The phone's private IPv4 address on its current Wi-Fi network, or null (cellular, VPN, offline). */
class WifiAddressProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalAddressProvider {

    override fun currentLocalAddress(): ByteArray? = try {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivity?.activeNetwork
        val capabilities = network?.let(connectivity::getNetworkCapabilities)
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            connectivity.getLinkProperties(network)?.linkAddresses
                ?.map { it.address }
                ?.firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                ?.address
        } else {
            null
        }
    } catch (e: SecurityException) {
        null
    }
}

object AndroidPairingLogger : PairingLogger {
    private const val TAG = "Pairing"

    override fun debug(message: String) = AppLog.d(TAG) { message }

    override fun warn(message: String, error: Throwable?) = AppLog.w(TAG, message, error)
}

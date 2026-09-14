package com.ecosystem.core.protocol

/**
 * Constants of the ConnectFlow pairing protocol, version 2.
 *
 * The normative specification is `ecosystem-shared-docs/protocol/PAIRING_PROTOCOL_V2.md`.
 * Every value here is part of the wire contract and must change on Android and macOS together.
 */
object ProtocolConstants {
    /** Carried in the QR (`v=2`), in every frame header and in the handshake transcript. */
    const val PROTOCOL_VERSION: Int = 2

    /** Domain-separation label used by the transcript, the HKDF info and every authentication input. */
    const val PROTOCOL_LABEL: String = "ConnectFlow-Pairing"

    const val X25519_KEY_SIZE = 32
    const val ED25519_PUBLIC_KEY_SIZE = 32
    const val ED25519_SIGNATURE_SIZE = 64
    const val NONCE_SIZE = 32
    const val DEVICE_ID_SIZE = 16
    const val HMAC_SHA256_SIZE = 32
    const val SESSION_KEY_SIZE = 32
    const val MAX_DEVICE_NAME_BYTES = 128
}

/** Purpose labels mixed into the HKDF info and into every authentication input. */
object ProtocolRoles {
    const val MAC_KEY_PURPOSE = "pairing-mac-key"
    const val ANDROID_AUTH = "android-auth"
    const val MAC_AUTH = "mac-auth"
    const val ANDROID_CONFIRM = "android-confirm"
    const val MAC_COMPLETE = "mac-complete"
}

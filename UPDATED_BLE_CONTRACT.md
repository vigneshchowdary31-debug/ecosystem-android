# Updated BLE GATT & Advertising Contract

This document specifies the Bluetooth LE (BLE) GATT database, advertising formats, and connection properties required to establish communications between Android and macOS.

---

## 1. BLE Roles & Connection Model

* **macOS:** Acts as the **GATT Server** and **BLE Advertiser**.
* **Android:** Acts as the **GATT Client** and **BLE Scanner**.
* Android initiates the connection to macOS once it resolves the macOS device's transient MAC address using the advertised UUID.

---

## 2. BLE Advertising Specifications (macOS)

Due to Apple CoreBluetooth platform constraints, `CBPeripheralManager` ignores custom manufacturer data keys (such as `CBAdvertisementDataManufacturerDataKey`) when broadcasting in peripheral mode. To ensure reliable discovery:

* **Advertising Identifier:** Although macOS generates and associates a 128-bit `advertisingIdentifier` UUID, it is **not** broadcasted in the BLE manufacturer data.
* **Service UUID:** macOS advertises the Continuity Service UUID `4F101A2B-3C4D-5E6F-7A8B-9C0D1E2F3A4B` to allow scanning clients to find the service.
* **Local Name:** macOS advertises the device name string. Android scanners must discover candidates by matching the Continuity Service UUID and filtering by the local device name matching the QR code payload (`qrData.name`).

---

## 3. BLE GATT Database Structure (macOS Server)

macOS hosts the following GATT Service and Characteristics:

| Service/Characteristic | UUID | Permissions | Properties |
| :--- | :--- | :--- | :--- |
| **Continuity Service** | `4F101A2B-3C4D-5E6F-7A8B-9C0D1E2F3A4B` | Read-only | — |
| **Write Characteristic** | `4F101A2C-3C4D-5E6F-7A8B-9C0D1E2F3A4B` | Write-only | `WRITE` / `WRITE_NO_RESPONSE` |
| **Notify Characteristic** | `4F101A2D-3C4D-5E6F-7A8B-9C0D1E2F3A4B` | Read-only | `NOTIFY` |
| **CCCD Descriptor** | `00002902-0000-1000-8000-00805f9b34fb` | Read/Write | — |

Android registers for notifications on the **Notify Characteristic** by writing `0x01, 0x00` (little-endian for notification enabled) to the Client Characteristic Configuration Descriptor (CCCD).

---

## 4. MTU Negotiation & Long Writes

1. **MTU Request:** Android requests an MTU size of **512 bytes** immediately upon connection state change to `CONNECTED`.
2. **Negotiated Fallback:** Both devices must negotiate the highest possible MTU up to 512 bytes. If a lower MTU (e.g. 185 or default 23 bytes) is returned, the connection falls back gracefully.
3. **macOS GATT Configuration:** 
   * macOS must support **Long Writes** (Write Long / Prepared Write requests) if the Android payload exceeds the negotiated MTU.
   * macOS CoreBluetooth automatically handles long writes if the characteristic helper options allow it, but the application must assemble the chunks if standard callbacks receive fragmented payloads.

---

## 5. Connection & Handshake Protocol Sequence

The pairing flow utilizes a passive-server model on macOS. macOS does **not** send any packets automatically immediately after connection or CCCD subscription. The communication must be initiated by the Android client.

### Step-by-Step Handshake Flow:

1. **Connection & Subscription:** Android discovers the macOS device via the Service UUID and Local Name matching, connects, requests MTU 512, and subscribes to the CCCD of the Notify Characteristic.
2. **Ephemeral Key Exchange (Initiation):** Android generates its ephemeral Curve25519 key pair and writes its public key (Base64) to the Write Characteristic.
3. **Ephemeral Key Response:** macOS generates its Curve25519 key pair, computes the shared secret, derives the session key (using HKDF-SHA256 with empty salt and info), and notifies Android with its macOS Ephemeral Public Key.
4. **Android Auth Request:** Android computes the session key, signs a random challenge with its Ed25519 private key, computes the HMAC-SHA256 session proof, and writes the `auth` payload to the Write Characteristic:
   `auth:<androidChallengeBase64>:<androidSignatureBase64>:<androidPublicKeyEd25519>:<androidSessionProofBase64>:<androidIpAddress>`
5. **macOS Auth Response:** macOS receives and verifies the Android signature and session proof, signs a new challenge with its Ed25519 private key, computes the HMAC-SHA256 session proof, and notifies Android with the `auth_resp` payload:
   `auth_resp:<macChallengeBase64>:<macSignatureBase64>:<macSessionProofBase64>:<macIpAddress>`
6. **Verification & Completion:** Android receives the notification, verifies macOS's signature on the challenge using the long-term `publicKeyEd25519` from the QR code, and verifies the macOS session proof. Once verified, both clients register the trusted device.

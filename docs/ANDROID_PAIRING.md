# Android pairing — architecture and operations

The wire protocol (QR format, BLE contract, framing, handshake, cryptography) is specified in
`ecosystem-shared-docs/protocol/PAIRING_PROTOCOL_V2.md`. This document covers how the
Android app implements it.

## Modules

```
:app                    Composition root (Hilt modules, port adapters), MainActivity,
                        debug tools in src/debug only
:feature:pairing        PairingViewModel, PairingUiState mapper, PairingScreen, QrScanner, QrCodeAnalyzer
:core:pairing   (JVM)   Pairing domain: PairingState, PairingFailure, PairingManager,
                        PairingCoordinator, PairingSession, ports (PairingTransport,
                        LocalIdentitySource, TrustedPeerStore, LocalAddressProvider, PairingLogger)
:core:protocol  (JVM)   Wire protocol: QR codec, TLV, frames + reassembler + fragmenter, ATT sizing,
                        messages, transcript, InitiatorHandshake, HandshakeCrypto (Tink)
                        test fixtures: InMemoryLink, ResponderHandshake (reference Mac)
:core:networking        BlePairingTransport (scanner), BleGattConnection (GATT client, one per session)
:core:identity          IdentityManager: device ID, advertising ID, identity key access
:core:security          KeystoreManager (AES-256-GCM in Android Keystore), SecureStorage, CryptoManager (Ed25519)
:core:trusteddevices    Room v3 database, migrations, TrustedDeviceRepository (domain model)
:core:discovery         BLE scan/advertise service used by the debug tools only
:core:common            BleConstants, BlePermissionHelper, AppLog, DispatcherProvider
```

Dependencies point inward: `:core:protocol` and `:core:pairing` have no Android, Bluetooth or DI
dependencies. The Android modules implement the pairing ports, and `:app/di/PairingModule`
assembles the `PairingCoordinator` from them.

```
PairingScreen ─► PairingViewModel ─► PairingManager (PairingCoordinator, app-scoped)
                                         │ one PairingSession at a time
                                         ├─► PairingTransport ◄── BlePairingTransport ─► BleGattConnection
                                         ├─► InitiatorHandshake (per session: ephemeral key, nonces, K, TH)
                                         ├─► LocalIdentitySource ◄── IdentityPairingSource ─► IdentityManager ─► CryptoManager ─► SecureStorage ─► KeystoreManager
                                         └─► TrustedPeerStore ◄── RoomTrustedPeerStore ─► TrustedDeviceRepository ─► Room
```

## Pairing session lifecycle

- `startPairing(qr)` parses the QR code; an invalid code fails immediately without touching
  Bluetooth. A scan while a session is active is ignored.
- The coordinator checks readiness (BLE hardware, Nearby-devices permission, Bluetooth on,
  and Location on for Android 11 and lower), loads the identity, then loops: scan → try
  candidates by RSSI → handshake. Transient failures (GATT 133, mid-handshake disconnect) are
  retried once per candidate; authentication failures and devices without the service are
  excluded. The whole session is limited to 60 s.
- Each attempt is a `PairingSession` owning the `BleGattConnection` and the
  `InitiatorHandshake`. `close()` runs in a `NonCancellable` block on success, failure and
  cancellation; it wipes the ephemeral private key and session key and closes the GATT.
- `cancelPairing()` cancels the job and shows `Cancelled`. `retryPairing()` cancels the running
  job, **waits for its cleanup**, then starts a clean session. Every state update carries a
  generation number, so a cancelled job can never overwrite a newer session's state.
- `PairingSucceeded` is emitted only after the Mac's final MAC_COMPLETE is verified **and**
  the Mac is stored. `Connected` (Mac verified, confirmation still in flight) is shown as
  progress, never as success.

## BLE transport

- Scanning: hardware filter on the service UUID, low-latency mode, first result plus 1.5 s to
  collect alternatives, at most 4 scan starts per 30 s. Candidates are sorted by RSSI. The
  address is only a connection handle.
- `BleGattConnection`: `connectGatt(autoConnect = false, TRANSPORT_LE)` on the `BluetoothDevice`
  from the scan result, MTU 517 requested (falls back to 23), service and characteristic
  checks, CCCD subscription, and an operation queue so only one GATT operation is outstanding.
  Every operation has a timeout. Late callbacks from a timed-out operation are dropped because
  a pending operation only accepts its own event type.
- Sending: frames are split into `min(MTU − 3, 512)`-byte packets, each sent as a Write Request
  whose response is awaited. Receiving: notifications feed a `FrameReassembler`; frames are
  complete only by their length field.
- Any failure, disconnect or cancellation closes the `BluetoothGatt`. `SecurityException`
  (permission revoked) maps to a permission failure instead of crashing.

## Security notes

| Value | Source |
|---|---|
| Ephemeral X25519 keys, handshake nonces | `TinkHandshakeCrypto(SecureRandom())` |
| Session IDs (logging only) | same `SecureRandom` |
| Ed25519 identity key | `Ed25519Sign.KeyPair.newKeyPair()` (Tink, backed by `SecureRandom`) |
| Device ID, advertising ID | `UUID.randomUUID()` (`SecureRandom`) |
| AES-GCM IVs | Android Keystore (randomised encryption enforced) |

- **Identity key protection is software-based.** Android Keystore has no Ed25519 at this
  app's minSdk, so the Ed25519 key is generated by Tink and stored only as a raw 32-byte seed
  sealed with an AES-256-GCM Android Keystore key (StrongBox when available, otherwise TEE).
  Each sealed value is bound to its preference key with AES-GCM associated data. The seed is
  decrypted into a byte array only to sign or derive the public key, then zeroed; Tink's
  signer keeps a derived scalar that cannot be wiped and becomes garbage right after use.
  Values from earlier builds are read and migrated on first use.
- **No silent identity rotation.** If identity material exists but cannot be decrypted (Keystore
  key lost), `IdentityKeyUnavailableException` is raised and pairing reports it; the identity is
  only replaced by an explicit `resetIdentity()`.
- **Backups are off** (`allowBackup=false`, `fullBackupContent=false`), and data-extraction rules
  exclude the secure preferences and the database from cloud backup and device-to-device transfer.
- **Logging** goes through `AppLog`, enabled only for debuggable builds; R8 also strips
  `Log.v/d/i` from release. Keys, session keys, signatures, raw frames and QR payloads are never logged.
- **Session keys are never persisted.** Only the Mac's public identity key, ID, name and an
  address hint are stored.
- The Mac does not yet authenticate Android (see the spec, §13).

## Persistence (Room v3)

`trusted_devices`: `deviceId` (lower-case UUID, primary key), `name`, `lastKnownIpAddress`,
`publicKeyEd25519` (Base64), `isTrustActive`, `pairingTimestamp`, `lastSeenTimestamp`.
`local_identity`: cached display name and creation time for this device.

| Version | Change | Migration |
|---|---|---|
| 1 | initial | — |
| 2 | `local_identity.advertisingIdentifier` | `MIGRATION_1_2` rebuilds `local_identity` |
| 3 | drop `trusted_devices.bleMacAddress`, lower-case device IDs | `MIGRATION_2_3` rebuilds `trusted_devices`, keeping every row |

- There is no destructive fallback. Schemas are exported to `core/trusteddevices/schemas/`.
- Upsert rules: re-pairing with the same key keeps the first pairing time and refreshes name,
  address and last-seen; the same device ID with a new key starts a new trust relationship.
- Rows that fail validation (non-canonical ID, key not 32 bytes, invalid name, zero
  timestamp) are skipped, never returned as trusted.
- `lastSeenTimestamp` and `lastKnownIpAddress` are set at pairing; `recordSeen()` updates them
  and is ready for Sprint 2 connections.

## Debug and release

- Release builds show only the pairing screen and are shrunk with R8.
- Debug builds add the Identity, BLE and Trusted-devices tools. They live in `app/src/debug`
  and reach dependencies through a Hilt `@EntryPoint`, so release code cannot reference them.

## Building

Configure the Android SDK the standard way: set `ANDROID_HOME`, or let Android Studio write
`local.properties` (never committed). No path is hard-coded in the repository.

```
./gradlew assembleDebug assembleRelease       # APKs (release is unsigned)
./gradlew testDebugUnitTest :core:protocol:test :core:pairing:test
./gradlew lintDebug
./gradlew :core:trusteddevices:connectedDebugAndroidTest :core:security:connectedDebugAndroidTest   # needs a device or emulator
```

After an intentional protocol change, regenerate the shared test vectors:

```
WRITE_VECTORS=../ecosystem-shared-docs/protocol/test-vectors/pairing-v2-vectors.json ./gradlew :core:protocol:test
cp ../ecosystem-shared-docs/protocol/test-vectors/pairing-v2-vectors.json core/protocol/src/test/resources/
```

## Manual device test checklist (Android + Mac running protocol v2)

1. Pair with the QR code of the Mac next to you. Expect: Paired, the Mac listed once in Trusted devices.
2. Scan the same QR code again. Expect: success, and the first pairing time is unchanged.
3. Scan a QR code from a different Mac that is out of range while the first Mac is nearby. Expect: "Couldn't verify the Mac" or "Couldn't find your Mac", and nothing stored.
4. Scan a legacy v1 code. Expect: "Update ConnectFlow on the Mac".
5. Turn Bluetooth off, then scan. Expect: "Bluetooth is off" → Turn on Bluetooth → pairing resumes.
6. Deny Nearby devices. Expect: "Allow Nearby devices"; after a second denial the button opens Settings.
7. Walk out of range or quit the Mac app mid-pairing. Expect: "The connection dropped" or "The Mac didn't respond"; then Try again works.
8. Tap Cancel during pairing. Expect: Cancelled, and the Mac sees the disconnect.
9. Pair, force-stop, reopen. Expect: the Mac is still trusted.
10. Pair a second Mac. Expect: both listed.
11. Rotate during pairing. Expect: the session continues and the state is kept.

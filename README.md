# Ecosystem Android

Android half of ConnectFlow: pairs with the macOS app over BLE (QR code + authenticated
X25519/Ed25519 handshake) and keeps a store of trusted Macs. Local Wi-Fi features come in
Sprint 2.

- Pairing protocol (shared with macOS): `../ecosystem-shared-docs/protocol/PAIRING_PROTOCOL_V2.md`
- Android architecture, security notes, database schema and manual test checklist: [docs/ANDROID_PAIRING.md](docs/ANDROID_PAIRING.md)
- Superseded v1 documents: [docs/legacy-v1/](docs/legacy-v1/)

## Build

Requirements: JDK 17 or newer and an Android SDK with platform 34. Point Gradle at the SDK
with `ANDROID_HOME`, or let Android Studio create `local.properties`. That file is
machine-specific and ignored by git.

```
./gradlew assembleDebug
./gradlew testDebugUnitTest :core:protocol:test :core:pairing:test
./gradlew lintDebug
./gradlew assembleRelease      # R8-shrunk, unsigned; sign outside the repository
```

Instrumented tests (Room migrations, Android Keystore) need a device or emulator:

```
./gradlew :core:trusteddevices:connectedDebugAndroidTest :core:security:connectedDebugAndroidTest
```

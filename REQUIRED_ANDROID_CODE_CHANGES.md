# Required Android Code Changes & Migration

This document outlines the exact files modified, before/after code comparisons, database migration strategy, and compatibility impacts for the Android application updates.

---

## 1. Files Modified

| File Path | Component | Changes Made |
| :--- | :--- | :--- |
| [AppDatabase.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/trusteddevices/src/main/java/com/ecosystem/core/trusteddevices/AppDatabase.kt) | `:core:trusteddevices` | Incremented database version to 2 to support Room destructive migrations. |
| [LocalIdentityEntity.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/identity/src/main/java/com/ecosystem/core/identity/LocalIdentityEntity.kt) | `:core:identity` | Added `advertisingIdentifier` database column. |
| [DeviceInfo.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/identity/src/main/java/com/ecosystem/core/identity/DeviceInfo.kt) | `:core:identity` | Added `advertisingIdentifier` parameter. |
| [IdentityManagerImpl.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/identity/src/main/java/com/ecosystem/core/identity/IdentityManagerImpl.kt) | `:core:identity` | Generated and persisted stable UUID `advertisingIdentifier` in secure storage & Room. |
| [DiscoveryService.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/discovery/src/main/java/com/ecosystem/core/discovery/domain/repository/DiscoveryService.kt) | `:core:discovery` | Updated `startAdvertising` signature to accept `advertisingIdentifier`. |
| [DiscoveryServiceImpl.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/discovery/src/main/java/com/ecosystem/core/discovery/data/ble/DiscoveryServiceImpl.kt) | `:core:discovery` | Advertised stable `advertisingIdentifier` in manufacturer data; parsed it during scan results. |
| [BleDebugScreen.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/app/src/main/java/com/ecosystem/android/BleDebugScreen.kt) | `:app` | Passed local `advertisingIdentifier` to `startAdvertising`. |
| [IdentityDebugScreen.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/app/src/main/java/com/ecosystem/android/IdentityDebugScreen.kt) | `:app` | Displayed `BLE Advertising ID` on validation card. |
| [QrPairingData.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/pairing/src/main/java/com/ecosystem/core/pairing/QrPairingData.kt) | `:core:pairing` | Replaced `bleMacAddress` and `ephemeralPublicKeyX25519` with `advertisingIdentifier`. |
| [PairingManagerImpl.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/pairing/src/main/java/com/ecosystem/core/pairing/PairingManagerImpl.kt) | `:core:pairing` | Scanned to resolve advertisingIdentifier, implemented Session Proof verify, and IP address exchange. |
| [build.gradle.kts](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/pairing/build.gradle.kts) | `:core:pairing` | Added dependency on `:core:discovery`. |
| [BleChannelImpl.kt](file:///Users/vigneshchowdary/Projects/EcosystemProject/ecosystem-android/core/networking/src/main/java/com/ecosystem/core/networking/BleChannelImpl.kt) | `:core:networking` | Requested MTU 512 immediately on connection, logging output. |

---

## 2. Before/After Code Comparison Examples

### A. QR Payload Parsing (PairingManagerImpl.kt)

#### Before:
```kotlin
    private fun parseQrPayload(payload: String): QrPairingData? {
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
```

#### After:
```kotlin
    private fun parseQrPayload(payload: String): QrPairingData? {
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
```

### B. Device Discovery Resolution via Scan (PairingManagerImpl.kt)

#### Before:
```kotlin
        val isConnected = bleChannel.connect(qrData.bleMacAddress)
```

#### After:
```kotlin
        discoveryService.startScanning(null)
        val resolvedDevice = withTimeoutOrNull(15000) {
            var found: com.ecosystem.core.discovery.domain.model.DiscoveredDevice? = null
            try {
                discoveryService.scanResults.collect { results ->
                    val match = results.firstOrNull { it.advertisingIdentifier.equals(qrData.advertisingIdentifier, ignoreCase = true) }
                    if (match != null) {
                        found = match
                        throw kotlinx.coroutines.CancellationException("Found matching device")
                    }
                }
            } catch (e: Exception) {}
            found
        }
        discoveryService.stopScanning()
        if (resolvedDevice == null) {
            _pairingState.value = PairingState.Failed("Failed to discover device with BLE Advertising ID: ${qrData.advertisingIdentifier}")
            return false
        }
        val isConnected = bleChannel.connect(resolvedDevice.macAddress)
```

### C. MTU 512 Request (BleChannelImpl.kt)

#### Before:
```kotlin
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.value = ChannelState.Connecting
                gatt.discoverServices()
            }
```

#### After:
```kotlin
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.value = ChannelState.Connecting
                android.util.Log.d("BLE_ADVERTISE_DEBUG", "Connected. Requesting MTU 512...")
                gatt.requestMtu(512)
            }
```

---

## 3. Database Migration Strategy

Since this project is in Sprint 1 development phase, we use **Room Destructive Migration** to handle schema updates cleanly. 
We incremented the version from `1` to `2` in `AppDatabase.kt`. 
When the updated application launches:
1. Room detects the version mismatch (`1` -> `2`).
2. Room drops the existing database tables (`local_identity` and `trusted_devices`) because `.fallbackToDestructiveMigration()` is enabled in `DatabaseModule.kt`.
3. Room creates new tables matching the new schema with the `advertisingIdentifier` column.
4. `IdentityManagerImpl` detects that the local identity is missing in the database. It regenerates the database row, loading the stable device ID and public key from Keystore-backed `SecureStorage`, and generating a stable `advertisingIdentifier` UUID.

---

## 4. Backward Compatibility & Impact

* **Breaking Change:** This update breaks backward compatibility with old Sprint 1 pairing clients because the QR code structure has changed (4 CSV fields instead of 5) and the BLE authentication handshake includes session proofs and IP addresses (5 colon-separated fields instead of 3).
* **Impact:** Since macOS development has not started yet, this change has zero production impact. All Android test clients will automatically migrate their databases and keys without manual intervention.

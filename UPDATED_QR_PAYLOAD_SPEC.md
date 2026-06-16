# Updated QR Payload Specification

This document details the QR code payload format used in the Continuity Pairing flow. It reflects the removal of physical MAC addresses and session-specific ephemeral keys from the QR code, using a stable advertising identifier instead.

---

## 1. Specification Format

The QR payload is represented as a comma-separated value (CSV) string prefixed with `identity_continuity:`:

```text
identity_continuity:<deviceId>,<name>,<advertisingIdentifier>,<publicKeyEd25519>
```

---

## 2. Field Details

| Field Index | Field Name | Format | Description |
| :--- | :--- | :--- | :--- |
| **Prefix** | `identity_continuity:` | String | Protocol identifier string to verify compatibility. |
| **1** | `deviceId` | UUID String | Long-term unique identifier of the device (UUIDv4). |
| **2** | `name` | Alpha-numeric | Human-readable name of the device (e.g. `Vignesh's MacBook Pro`). |
| **3** | `advertisingIdentifier` | UUID String | Stable UUIDv4 used as the BLE advertising key to discover the device without exposing its physical MAC address. |
| **4** | `publicKeyEd25519` | Base64 String | Base64-encoded long-term Ed25519 public key of the device (`Base64.NO_WRAP`). |

---

## 3. Rationale for Changes

1. **MAC Address Privacy:** Apple's CoreBluetooth framework on macOS does not expose the device's physical BLE MAC address to developers to prevent user tracking. The introduction of `advertisingIdentifier` (a stable UUID) allows Android to discover and resolve the transient macOS MAC address dynamically via BLE scan matching.
2. **Ephemeral Key Security:** Previously, an ephemeral X25519 public key was encoded in the QR code. Ephemeral keys must be generated dynamically per session. Storing them in a static QR code is a security vulnerability and leads to replay attacks. The X25519 key exchange is now performed exclusively over the established BLE link.

---

## 4. Example Payload

```text
identity_continuity:2d861d8a-6b83-4a1d-8f9c-76e48c3f8f11,MacBook Pro,f7823ab4-3e9a-4c28-be51-24b893a201f9,u514c3+fG56MzdV8q2+wKWhS5g7Y2Lq6m1d3P5R7Z8Q=
```

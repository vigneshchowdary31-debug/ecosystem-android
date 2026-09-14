# ConnectFlow Android — Sprint 2 Design: Universal Clipboard

| | |
|---|---|
| Status | Android implementation design. No production code exists for anything below. |
| Protocol authority | [`ecosystem-shared-docs/protocol/SYNC_CHANNEL_V1.md`](../../../ecosystem-shared-docs/protocol/SYNC_CHANNEL_V1.md) at `develop` `f12dc3f`: the **single source of truth** for every cross-platform decision |
| Baseline | `ecosystem-android` `feature/pairing` @ `df79811` (Sprint 1 closed and frozen) |
| Revision | 2 (14 Sep 2026). The cross-platform definitions of revision 1 now live in the shared spec; see Appendix A. |

**How to read this document**

- **[SPEC §n]** means the behaviour, value or byte layout is defined in `SYNC_CHANNEL_V1.md` §n. This document only says how Android implements it, and doesn't restate spec values as Android decisions.
  - If this document and the spec ever disagree, **the spec wins** and this document has a bug.
  - Changing anything tagged [SPEC] needs a new channel version, agreed by both platforms (spec §18).
- **[ANDROID]** is an Android-only decision. It can change without involving macOS, as long as the spec is still met.
- Byte encodings, device IDs, TLV strictness and u64 conventions are those of spec §1.3.

**Constraints inherited from Sprint 1 (unchanged by Sprint 2)**

- **Read-only for Sprint 2:** Protocol V2, BLE pairing, the `trusted_devices` schema, `IdentityManager`, `KeystoreManager`, `CryptoManager`, `TrustedDeviceRepository`, `HandshakeCrypto`, `:core:networking` and `:core:pairing`.
- **Toolchain:** `minSdk 26`, `compileSdk 34`, `targetSdk 34`, AGP 8.3.2, Kotlin 1.9.24, JDK 17 bytecode.
- **BLE never carries clipboard data** [SPEC §1.2].
- **The only Sprint 1 API Sprint 2 writes through** is `TrustedDeviceRepository.recordSeen()` (§2.4). That existing API was unused until now.

---

## 0. Decision summary

| Topic | Android implementation | Authority |
|---|---|---|
| Roles, transport, IPv4 only, no BLE data | Android is the WebSocket client; OkHttp is the client | [SPEC §1.2, §4] |
| Discovery | `NsdManager` browse with API-split resolve, the fast path, endpoint cache, `recordSeen()` | [SPEC §3] + [ANDROID] §2 |
| Secure channel and messages | Initiator handshake. Existing `HandshakeCrypto` for X25519, HKDF and Ed25519; `javax.crypto` for AES‑256‑GCM. | [SPEC §5–§8] + [ANDROID] §4 |
| Clipboard content rules | Plain text and URLs, validity, URL rule, hash, sensitive clips | [SPEC §9] |
| Capture UX | Focused capture, notification action, Quick Settings tile, in-app button, share sheet | [ANDROID] §5 |
| Receive and apply | Processing order from the spec. `setPrimaryClip` on the main thread. | [SPEC §10.4] + [ANDROID] §6 |
| Loops, duplicates, ordering | Lamport counter, current synced state, seen set, origin-only sending | [SPEC §10] |
| Outbox | Latest item only, TTL, retry reusing the same item | [SPEC §11] + [ANDROID] §8.3 |
| Authorization and revocation | One active sync Mac, trust flow observed, 4003 on revoke | [SPEC §12] + [ANDROID] §8.4 |
| Timing and close codes | Values from the spec, mapped to OkHttp and coroutine timeouts | [SPEC §13, §14] |
| Client reconnection | The spec's client policy, triggered by Android events | [SPEC §14.2] + [ANDROID] §3.4 |
| Background execution | `connectedDevice` foreground service, user-visible starts only, no boot start | [ANDROID] §9 |
| History | Limits from the spec. Separate Room DB, Keystore-sealed content. | [SPEC §16] + [ANDROID] §10 |
| Testing | Android tests, plus the spec's conformance requirements | [SPEC §15] + [ANDROID] §13 |

---

## 1. Network architecture

- **Roles:** Android is the WebSocket client and handshake initiator; the Mac is the server [SPEC §1.2].
- **Data path:** clipboard data travels only over local IPv4 Wi‑Fi, inside the Sync Channel. BLE stays pairing-only, and no Sprint 1 BLE code or GATT characteristic changes [SPEC §1.2].
- **Why Android is the client** [ANDROID rationale, consistent with the spec]: Android can't reliably keep a listening socket in the background. Doze, app standby and OEM battery managers see to that, and no platform exemption covers it.
- **Active sync Mac** [SPEC §12.1, ANDROID UX]: the user picks one "active sync Mac" among Android's trusted Macs (`isTrustActive = 1`) on the Clipboard screen. Other trusted Macs stay paired but don't sync.

---

## 2. Device discovery (implements SPEC §3)

### 2.1 What Android consumes

- **Defined by the spec:** the service type, TXT keys, `idh` derivation and default port [SPEC §3.1–§3.4]. Android implements the endpoint-selection procedure in [SPEC §3.5].
- **Computing `idh`:** Android computes the expected `idh` of the active sync Mac from its stored `deviceId`, as defined in [SPEC §3.3]. The value must match the shared vectors (`discovery.idh`).

### 2.2 NSD implementation [ANDROID]

- **Browse:** `NsdManager.discoverServices("_connectflow._tcp", NsdManager.PROTOCOL_DNS_SD, listener)`.
- **Resolve:**
  - API 34+: `registerServiceInfoCallback(serviceInfo, executor, callback)`. It also delivers `onServiceUpdated` (address change) and `onServiceLost`.
  - API 26–33: `resolveService(serviceInfo, listener)`. Only one resolve may be outstanding (`FAILURE_ALREADY_ACTIVE`), so found services go through a serial resolve queue.
- **TXT parsing:** from `NsdServiceInfo.getAttributes()`, following the TXT rules in [SPEC §3.2]: both keys required, `v` must list `1`, unknown keys ignored, and `idh` must equal the computed value.
- **Address:** the first `Inet4Address` of the resolved host. With no IPv4 address, the service is skipped [SPEC §3.5].
- **Multicast:** no `MulticastLock` is needed; `NsdManager` uses the system mDNS responder.
- **When discovery runs:** only in the `Discovering` state (§3.3). Each attempt is capped by the spec's NSD window [SPEC §13]. Discovery stops once a session is established.

### 2.3 Endpoint selection [SPEC §3.5], Android mechanics [ANDROID]

- **Parallel paths:** the fast-path candidates and NSD run as parallel coroutines under one attempt scope. The first connection to complete the handshake cancels the others.
- **Fast-path candidates, in spec order:**
  1. `peer_endpoints.host:port` for this Mac (§10.2)
  2. `trusted_devices.lastKnownIpAddress` with the default port, used only when it is IPv4 and differs from candidate 1
- **Connect timeouts:** the spec's fast-path and normal values [SPEC §13].
- **What an endpoint proves:** nothing. Identity comes only from the handshake [SPEC §3.5].

### 2.4 Endpoint cache and `recordSeen()` [ANDROID]

After every established session:
- write `peer_endpoints(peerDeviceId, host, port, updatedAt)`
- call `TrustedDeviceRepository.recordSeen(deviceId = macId, timestamp = now, ipAddress = host)`

`recordSeen()` touches only `lastSeenTimestamp` and `lastKnownIpAddress`, the "routing hint for Sprint 2" the entity documents. The schema, trust, keys and pairing time stay unchanged. The spec permits recording the address [SPEC §3.5].

### 2.5 Reacting to address changes [ANDROID]

| Change | Android detection | Action |
|---|---|---|
| The Mac's address changes | Socket failure or a missed pong. On API 34+ also NSD `onServiceUpdated`. | Close, then re-enter `Discovering` |
| The phone's IPv4 address changes | `NetworkCallback.onLinkPropertiesChanged` | Close 1001, reconnect at once (an immediate trigger, [SPEC §14.2]) |
| The phone joins another Wi‑Fi network | `onAvailable` for a new `Network`, or `onLost` for the current one | Close, ignore the cached endpoint for this attempt, discover again |

---

## 3. WebSocket transport on Android (implements SPEC §4, §13, §14)

### 3.1 OkHttp configuration [ANDROID]

- **URL, subprotocol and extensions:** as in [SPEC §4.1]. After `onOpen`, Android checks the response:
  - `Sec-WebSocket-Protocol` must be the spec's subprotocol, else close 1002.
  - A `Sec-WebSocket-Extensions` header means close 1002.
- **Frames:** binary only. A text frame means close 1003 [SPEC §4.2].
- **Pings:** OkHttp `pingInterval` is set to the spec's ping interval [SPEC §4.2, §13]. OkHttp fails the connection when a pong doesn't arrive before the next ping. Pings sent by the Mac are answered automatically by OkHttp.
- **Size guard:** an incoming message over the spec's record or handshake limits closes 1009 before any decryption or parsing [SPEC §4.3]. Android never sends over those limits either.
- **Network binding:** the client uses the Wi‑Fi `Network`'s `socketFactory` (§8.2). Otherwise Android may route traffic over cellular when Wi‑Fi has no internet.
- **Cleartext:**
  - The spec uses `ws`, not `wss` [SPEC §4.1]. Android's cleartext policy therefore needs `res/xml/network_security_config.xml` with `base-config cleartextTrafficPermitted="true"`, because dynamic LAN addresses can't be listed.
  - The app has no other HTTP traffic.
  - A unit test fails the build if any `http://` or `ws://` URL other than the sync endpoint template appears in the code.

### 3.2 Timeouts [SPEC §13], mapped to Android

| Spec item | Android mechanism |
|---|---|
| Fast-path / normal TCP connect | OkHttp `connectTimeout` per candidate |
| WebSocket upgrade | Coroutine timeout from connect to `onOpen` |
| Each handshake step, and the whole handshake | `withTimeout` around each receive, and around C1 → `READY` (close 4005) |
| ACK wait | Per-item timer. On expiry: close 1001 and reconnect [SPEC §11]. |
| Maximum session | Timer from establishment. On expiry: close 1000 and reconnect [SPEC §4.4]. |
| NSD attempt | Attempt scope timeout (§2.2) |

### 3.3 Connection state machine [ANDROID]

```
Idle ──(sync enabled + Wi‑Fi available + trusted active Mac)──▶ Discovering
Discovering ──(endpoint)──▶ Connecting ──(upgrade ok)──▶ Authenticating ──(READY)──▶ Connected
any state ──(failure)──▶ Backoff ──(timer / trigger)──▶ Discovering
any state ──(Wi‑Fi lost)──▶ WaitingForNetwork ──(Wi‑Fi available)──▶ Discovering
any state ──(close 4001 / 4003 / 4007)──▶ Blocked ──(user action)──▶ Discovering
any state ──(sync disabled / trust revoked / service stopped)──▶ Idle
```

- **One connection at a time,** to the active sync Mac.
- **Teardown** closes the socket, wipes the session keys (§4.4) and stops NSD.
- **Mac-side replacement** of an older connection (4004) is defined in [SPEC §14.1].

### 3.4 Reconnection [SPEC §14.2], Android triggers [ANDROID]

- **Delays, jitter, reset rule and blocking codes:** as in [SPEC §14.2].
- **Android events that act as the spec's "reconnect at once" triggers:**
  - `NetworkCallback.onAvailable`, or an IPv4 change
  - an NSD match
  - `MainActivity` coming to the foreground
  - any "Send clipboard" action
- **Status texts:**

| Close code | Status shown | Leaves `Blocked` on |
|---|---|---|
| 4001 | "Your Mac no longer trusts this phone — pair again" | User action |
| 4003 | "Clipboard sync is off on the Mac" [SPEC §12.4] | User action: opening ConnectFlow, tapping Send or tapping Retry |
| 4007 | "Update ConnectFlow" | User action |

---

## 4. Secure channel on Android (implements SPEC §5–§8)

### 4.1 Scope

- **Defined by the spec, not restated here:** wire message formats, handshake messages, transcript, authentication inputs, purpose strings, key schedule, record layout, nonce, AAD, sequence rules, application messages, `ACK` result codes and close codes [SPEC §5–§8, §14.1, Appendix A].
- **Android's role:** the initiator. It performs exactly the Android checks, in the order of [SPEC §6.6].
- **The Sprint 1 BLE session key** is never reused [SPEC §2].

### 4.2 Key sources [ANDROID]

| Spec key | Android source |
|---|---|
| `skA` | Never exposed. Signing goes through `IdentityManager.sign()`, wrapped as the protocol's `IdentitySigner`. |
| `pkA`, `idA` | `IdentityManager.getOrCreateIdentity()`: `DeviceInfo.publicKeyEd25519` (Base64, 32 bytes) and `deviceId` |
| `pkM`, `idM` | `TrustedDeviceRepository.getDevice(activeMacId)`: `publicKeyEd25519` (Base64, 32 bytes) and `deviceId`. `isTrustActive` must be true. |
| `ephA`, `nonceA` | `HandshakeCrypto.generateX25519KeyPair()` / `randomBytes(32)`, fresh per connection |

### 4.3 Primitive mapping [ANDROID]

| Spec primitive | Android implementation |
|---|---|
| X25519 with all-zero rejection | `HandshakeCrypto.x25519` (Tink, already rejects all-zero secrets) |
| HKDF‑SHA256, zero-length salt | `HandshakeCrypto.hkdfSha256(ikm, salt = ByteArray(0), info, 32)`. Proven compatible by the Pairing V2 vectors. |
| SHA‑256 | `HandshakeCrypto.sha256` |
| Ed25519 verify | `HandshakeCrypto.verifyEd25519` |
| Ed25519 sign | `IdentityManager.sign()` (Tink, deterministic RFC 8032). Android therefore reproduces the vector signatures byte for byte [SPEC §15.3]. |
| AES‑256‑GCM | `javax.crypto.Cipher("AES/GCM/NoPadding")` with `GCMParameterSpec(128, nonce)` and `updateAAD`. Available on every API level from minSdk 26. |

No new crypto library is added, and no Sprint 1 class changes.

### 4.4 Android handling rules [ANDROID, within SPEC §6–§8]

- **Key lifetime:** `ss`, the ephemeral private key and both session keys are held in `ByteArray`s, wiped (zero-filled) as the spec requires [SPEC §6.5, §7.4], and never logged (`AppLog` rule).
- **What Android sends:** C1 and C2, then only after `READY`, `CLIP` and `ACK` records [SPEC §6.6, §7.3]. It never sends `READY`.
- **`NOT_ACCEPTED`:** Android has no "pause receiving" feature in Sprint 2, so it never sends `NOT_ACCEPTED`. When it receives one, it removes the item from the outbox and tells the user [SPEC §8.4, §11].

---

## 5. Clipboard capture (Android → macOS) [ANDROID]

### 5.1 Platform rules this design is built on

| Rule | Consequence |
|---|---|
| **API 29+:** only the app with input focus, or the default keyboard, can read the clipboard. `getPrimaryClip()` returns null otherwise, and change listeners aren't delivered while unfocused. | ConnectFlow can't capture copies made in other apps in the background. There are no workarounds: no accessibility service, no custom keyboard, no polling. |
| **API 31+:** the system shows "ConnectFlow pasted from your clipboard" when the app reads content another app placed | Read only on an explicit user action or an in-app change, never on app open |
| **API 33+:** `ClipDescription.EXTRA_IS_SENSITIVE` | Sensitive clips are never captured [SPEC §9.5]. On API 26–32 the flag doesn't exist and can't be honoured. |
| Writing (`setPrimaryClip`) is not focus-gated | Receiving works in the background (§6) |

### 5.2 What is automatic and what is not

| Situation | Behaviour |
|---|---|
| The user copies text inside ConnectFlow (for example with "Copy" on a history item) while it is focused | **Automatic.** A `ClipboardManager.OnPrimaryClipChangedListener`, registered in `MainActivity.onResume` and removed in `onPause`, captures it. |
| The user opens ConnectFlow | **Not automatic.** The clipboard isn't read on open, so there's no read toast and nothing is sent silently. The Clipboard screen offers "Send clipboard". |
| The user copies in another app while ConnectFlow is in the background | **Not automatic** (platform restriction). The user sends it with one of the triggers below. |
| **Trigger:** the "Send clipboard" action on the sync notification | User-triggered, through `SendClipboardActivity` |
| **Trigger:** the "Send clipboard" Quick Settings tile | User-triggered, through `SendClipboardActivity` |
| **Trigger:** the "Send clipboard" button on the Clipboard screen | User-triggered. Read directly, because the activity is focused. |
| **Trigger:** Share sheet → "Send to Mac" (`ACTION_SEND`, `text/plain`) | User-triggered. Uses `EXTRA_TEXT`; the clipboard isn't read. |
| The Mac sends an item | **Automatic** while the service is connected (§6) |

These capture rules are the Android behaviour the spec refers to [SPEC §9.6].

**`SendClipboardActivity`:**
- **Appearance:** translucent theme, `excludeFromRecents`, `noHistory`, `taskAffinity=""`.
- **When it reads:** only in `onWindowFocusChanged(hasFocus = true)`, because a read in `onCreate` or `onResume` can return null before focus is granted.
- **What it does next:**
  - It hands the text to the sync service, starting the service if needed; that's allowed because the activity is in the foreground.
  - It shows the result for about 1 s: "Sent to <Mac>", "Queued — Mac not reachable", "Sensitive content isn't sent" or "Too large to send (limit 256 KB)".
  - Then it finishes.
- **The tile** launches it with `startActivityAndCollapse(PendingIntent)` on API 34+, and `startActivityAndCollapse(Intent)` below that.

### 5.3 `ClipboardManager` usage

- **Obtain:** `context.getSystemService(ClipboardManager::class.java)`.
- **Read:**
  - `primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()`
  - Only item 0 and only `getText()`. `coerceToText()` is never called, because it can resolve content URIs.
  - Clips holding only a URI or an Intent are ignored with "Nothing to send", since the spec excludes URI and Intent payloads [SPEC §9.1].
  - HTML clips contribute their plain-text `getText()`; formatting is dropped [SPEC §9.1].
- **Sensitive:** if `primaryClipDescription.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE)` is true, don't capture [SPEC §9.5].
- **Write:** see §6.2.

### 5.4 Content rules

- **Defined by the spec:** scope, text validity, URL classification and `contentHash` [SPEC §9.1–§9.4].
- **Android-side details** [ANDROID]:
  - Text is converted from the platform `String` (UTF-16) to UTF-8 with a strict encoder. A string with unpaired surrogates fails to encode and is refused locally.
  - Whitespace-only text isn't sent (the spec's sender SHOULD).
  - Text over the spec's size limit isn't sent, and the user sees "Too large to send (limit 256 KB)".

### 5.5 Android → macOS flow

1. **Trigger (§5.2)** produces text and a flag saying whether it is explicit or automatic.
2. **Validate** locally (§5.4). On failure, the user sees the reason and the flow ends.
3. **Skip echoes:** apply the automatic-capture and explicit-send rules of [SPEC §10.2]. An automatic capture equal to the current synced state isn't sent.
4. **Stamp the item** as [SPEC §10.2] requires (`L`, `lamport`, `messageId`, `createdAt`, update `C`). Write a history row, direction `SENT` (§10).
5. **Queue** it in the outbox [SPEC §11] (§8.3).
6. **If connected:** send the `CLIP` and start the ACK timer [SPEC §8.4, §11].
7. **Handle the `ACK`** per [SPEC §8.4, §11]. For `TOO_LARGE`, `INVALID` or `NOT_ACCEPTED`, the user sees the reason.
8. **If not connected:** the item waits in the outbox, and the send is an immediate reconnect trigger (§3.4).

---

## 6. Clipboard receive (macOS → Android)

### 6.1 Processing

- **When:** items are received only while `ClipboardSyncService` is running and a session is established.
- **Order:** every incoming `CLIP` goes through the spec's receive processing order, step for step, including every `ACK` outcome [SPEC §10.4].
- **While disconnected:** Android receives nothing. The Mac keeps at most its latest pending item, subject to the spec's TTL [SPEC §11].

### 6.2 Apply [ANDROID]

- **Write:** on the main thread, `clipboardManager.setPrimaryClip(ClipData.newPlainText("ConnectFlow", text))`.
  - URL items are written as plain text too; no Intent or URI is attached [SPEC §9.6].
  - The current synced state is updated **before** this call [SPEC §10.4, step 11]. So when ConnectFlow is focused, the in-app listener sees an equal hash and does nothing.
- **Afterwards:** write a history row (direction `RECEIVED`, §10), then send `ACK APPLIED`.
- **Feedback:** on API 33+, the system's own copy confirmation is the only feedback; the app adds no toast. On API 26–32 nothing is shown. The notification text changes to "Last received from <Mac> at hh:mm".
- **URLs are never opened automatically** [SPEC §9.6]. The history screen offers "Open" as an explicit tap, for `http` and `https` only.

---

## 7. Duplicate and loop prevention (implements SPEC §10)

- **What the spec defines** (Android adds no rule of its own):
  - the persisted state `L`, the current synced state `C`, and the seen set [SPEC §10.1]
  - origin-only sending and the automatic-capture rules [SPEC §10.2]
  - Lamport ordering, including the unsigned tie-break [SPEC §10.3]
  - the receive order [SPEC §10.4]
  - the loop argument [SPEC §10.5]
- **Android storage** [ANDROID]:
  - `L` and `C` live in `sync_state`, with `currentHash` sealed with `KeystoreManager` (§10.2, §10.3).
  - The seen set lives in `seen_messages`, sized as the spec requires, evicting the oldest.
  - Comparisons of `originDeviceId` use unsigned byte values; Kotlin `Byte` is signed, so bytes are masked with `and 0xFF`. The vector case `0x80 > 0x7f` covers this.
- **Clipboard changes Android can't see** [ANDROID note]:
  - Copies made in other apps while ConnectFlow is unfocused aren't observable.
  - A later Mac item overwrites them, which is expected universal-clipboard behaviour.

---

## 8. Offline and reconnection [ANDROID, within SPEC §11, §13, §14]

### 8.1 States

- **The states:** `Idle`, `WaitingForNetwork`, `Discovering`, `Connecting`, `Authenticating`, `Connected`, `Backoff` and `Blocked` (§3.3).
- **Where they're shown:** the notification and the Clipboard screen, in words:
  - "Connected to <Mac>"
  - "Looking for <Mac>"
  - "Waiting for Wi‑Fi"
  - "Mac not reachable — retrying in 16 s"
  - the `Blocked` texts of §3.4

### 8.2 Network monitoring

- **Callback:** `ConnectivityManager.registerNetworkCallback(NetworkRequest(TRANSPORT_WIFI), callback)`, held by the service.
- **`onAvailable(network)`:** remember `network`, then move to `Discovering` without backoff.
- **`onLost(network)`** for the remembered network: close 1001, move to `WaitingForNetwork`, stop NSD.
- **`onLinkPropertiesChanged`:** if the IPv4 address differs from the one the session started on, close 1001 and move to `Discovering` without backoff.
- **Socket binding:** all sync sockets use the remembered network's `socketFactory`, so LAN traffic never follows a cellular default network.
- **Cellular only, or no network:** stay in `WaitingForNetwork`; no discovery and no retries.

### 8.3 Outbox [SPEC §11], Android mechanics

- **Defined by the spec:** latest item only, the TTL, in-memory only, sending after `READY`, byte-identical retries, and removal on any `ACK` or on revoke.
- **Android mechanics** [ANDROID]:
  - The TTL is measured with `SystemClock.elapsedRealtime()`.
  - The outbox lives in the service's memory, so it is lost if the process dies, as the spec allows.
  - When an item from an **explicit** send expires, the notification says "Not sent — <Mac> wasn't reachable".

### 8.4 Situations

| Situation | Android behaviour |
|---|---|
| Wi‑Fi lost | Close 1001, `WaitingForNetwork`, keep the pending item until its TTL |
| Moving between Wi‑Fi networks | Close, discover on the new network, never reuse a session |
| The phone's IPv4 address changes | Close, reconnect at once (§2.5) |
| Mac asleep, off or unreachable | Backoff [SPEC §14.2], NSD attempts capped [SPEC §13], status "Mac not reachable" |
| The Mac's address changes | The socket or ping fails, so rediscover (§2.5) |
| WebSocket drops | Keys wiped [SPEC §7.4]. Back off, run a fresh handshake, resend the pending item unchanged [SPEC §11]. |
| App process restarts | Restore `L`, `C` and the seen set from `connectflow_sync.db`. No automatic send. |
| Doze or app standby | The foreground service keeps the process state that keeps network access (§9.5). If the OS or OEM still cuts the socket, a missed pong leads to reconnection. |
| The Mac is revoked or deleted on Android | Close **4003**, wipe keys, remove the pending item, stop syncing with it, delete its history and endpoint cache [SPEC §12.1] |
| The Mac closes with 4003 (switch off, or phone revoked on the Mac) | `Blocked`, "Clipboard sync is off on the Mac". Retry only on user action [SPEC §12.4]. |

---

## 9. Background execution [ANDROID]

### 9.1 Service

`ClipboardSyncService` is a foreground service, in `:app`.

- **Manifest:** `android:foregroundServiceType="connectedDevice"`, `android:exported="false"`.
- **Permissions (all normal or install-time, except the notification permission):**
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_CONNECTED_DEVICE` (required for the type at targetSdk 34)
  - `CHANGE_NETWORK_STATE`: satisfies Android 14's requirement that a `connectedDevice` service holds one of the listed prerequisite permissions
  - `INTERNET` (sockets and NSD)
  - `ACCESS_NETWORK_STATE` (already declared)
- **`startForeground`** is called immediately in `onStartCommand`, with `ServiceCompat.startForeground(…, FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)`.
- **Why `connectedDevice`:**
  - Android defines it for interaction with external devices over a network connection.
  - `dataSync` is capped at 6 hours a day once the app targets API 35.
  - `specialUse` needs a Play review justification.

### 9.2 Starting and stopping

- **Starts only from user-visible contexts,** because Android 12+ forbids starting a foreground service from the background:
  - the user enables sync on the Clipboard screen
  - app launch while sync is enabled
  - `SendClipboardActivity` (notification action or tile)
  - `ShareToMacActivity`
- **No automatic start at boot in Sprint 2.** After a reboot, sync resumes when the user opens ConnectFlow or uses the tile.
- **`onStartCommand` returns `START_STICKY`.** Restart behaviour on One UI is verified by test B‑4.
- **Stops when:**
  - the user taps "Stop sync" (this also sets `syncEnabled = false`)
  - the active Mac is revoked or deleted
  - no trusted active Mac exists
- **Not used in Sprint 2:** Companion Device Manager (it would add a second pairing-like flow, and BLE pairing is frozen), WorkManager, alarms and wake locks.

### 9.3 Notification

- **Channel:** `clipboard_sync`, "Clipboard sync", `IMPORTANCE_LOW`.
- **Content:** the connection state (§8.1).
- **Actions:** "Send clipboard" (a `PendingIntent` to `SendClipboardActivity`) and "Stop sync".
- **Permission:** `POST_NOTIFICATIONS` (API 33+) is requested when the user enables sync. If it's denied, the service still runs without a visible notification, and the Clipboard screen explains that the tile and in-app button still work.

### 9.4 Target SDK

- **Now:** written for `targetSdk 34`.
- **Raising to 35 or 36 later:**
  - `connectedDevice` has no runtime cap, so nothing in §9 changes.
  - Google has announced that local-network access will need a runtime permission for apps targeting a future SDK; Android 16 ships local-network protection as an opt-in behaviour. The permission request is added when `targetSdk` is raised, and test D‑6 exercises the opt-in behaviour now.

### 9.5 Doze, standby and OEM battery management

- **Doze:** while the foreground service runs, the app keeps network access during Doze and app standby. Verified by tests D‑1 to D‑3.
- **Samsung:** "Sleeping apps" / "Deep sleeping apps" can still kill the process.
  - The Clipboard screen shows a one-time "Keep sync running" guide that opens `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`.
  - The app does not request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, because of Play policy.
- **Keep-alive:** the spec's ping keeps the link alive, and reconnection covers gaps.

---

## 10. Clipboard history and sync state storage [ANDROID, limits from SPEC §16]

### 10.1 Database

- **Database:** a separate Room database, `connectflow_sync.db`, in `:core:syncstore`. It is never added to the Sprint 1 `AppDatabase`, so the trust store stays out of Sprint 2 migrations.
- **Schema rules:** version 1, `exportSchema = true` (`core/syncstore/schemas/`), explicit migrations for later versions, no destructive fallback.
- **Backup exclusion:**
  - The existing `data_extraction_rules.xml` excludes only named files.
  - Sprint 2 adds `connectflow_sync.db`, `connectflow_sync.db-wal` and `connectflow_sync.db-shm` to both `<cloud-backup>` and `<device-transfer>`.
  - `allowBackup="false"` is already set.

### 10.2 Schema

```
clip_history
  messageId        TEXT    PRIMARY KEY      -- 16 bytes as lower-case 8-4-4-4-12 hex
  peerDeviceId     TEXT    NOT NULL
  originDeviceId   TEXT    NOT NULL
  direction        TEXT    NOT NULL CHECK (direction IN ('SENT','RECEIVED'))
  kind             TEXT    NOT NULL CHECK (kind IN ('TEXT','URL'))
  content          BLOB    NOT NULL         -- sealed, see 10.3
  sizeBytes        INTEGER NOT NULL
  lamport          INTEGER NOT NULL
  createdAt        INTEGER NOT NULL         -- origin clock, ms
  recordedAt       INTEGER NOT NULL         -- local clock, ms
  INDEX (recordedAt)

seen_messages                               -- the seen set of SPEC §10.1
  messageId        TEXT    PRIMARY KEY
  peerDeviceId     TEXT    NOT NULL
  seenAt           INTEGER NOT NULL
  INDEX (peerDeviceId, seenAt)

sync_state          -- exactly one row, id = 1; L and C of SPEC §10.1
  id               INTEGER PRIMARY KEY CHECK (id = 1)
  activePeerId     TEXT
  syncEnabled      INTEGER NOT NULL
  historyEnabled   INTEGER NOT NULL DEFAULT 1
  lamport          INTEGER NOT NULL DEFAULT 0
  currentHash      BLOB                     -- sealed, see 10.3
  currentLamport   INTEGER NOT NULL DEFAULT 0
  currentOriginId  TEXT

peer_endpoints
  peerDeviceId     TEXT    PRIMARY KEY
  host             TEXT    NOT NULL         -- IPv4 literal
  port             INTEGER NOT NULL
  updatedAt        INTEGER NOT NULL
```

The `lamport` columns hold values below 2^63 [SPEC §8.3], so they fit Kotlin `Long` and SQLite `INTEGER`.

### 10.3 Encryption

- **Sealed with the existing `KeystoreManager.encrypt(plaintext, associatedData)`:**
  - `clip_history.content`, with associated data `"connectflow_sync/clip_history/" + messageId`
  - `sync_state.currentHash`, with associated data `"connectflow_sync/sync_state/currentHash"`
- **What that gives:** AES‑256‑GCM under the app's Android Keystore key. This meets the spec's at-rest recommendation [SPEC §10.1, §16].
- **No changes to `KeystoreManager`.**
- **Undecryptable data:** rows that fail to decrypt are deleted. A lost `currentHash` is reset to null, which the spec treats as "no current state" [SPEC §10.3].

### 10.4 Retention and clearing

- **Limits:** the spec's maximum entries and maximum age [SPEC §16].
- **Enforcement:** in the same transaction as every insert, plus a sweep when the service starts.
- **"Clear history":** deletes all rows.
- **"Keep history" off:** sets `historyEnabled = 0`, deletes all rows and stops writing. The sync state and the seen set are unaffected.
- **Revoking or deleting a Mac:** deletes its history rows, its endpoint and its seen entries.
- **`IdentityManager.resetIdentity()`:** deletes the whole database contents.
- **Local only:** history is never synced or replayed [SPEC §16].

---

## 11. Security

- **Threat model and properties:** defined by [SPEC §17], including the Mac's default-off "Allow Clipboard Sync" switch [SPEC §12.2] and the known gaps.
- **Android's responsibilities within it** [ANDROID]:

| Responsibility | Android measure |
|---|---|
| Authenticate the Mac | Verify `sigM` only with the **stored** `pkM` of the active sync Mac. Discovery data is never trusted. [SPEC §6.6] |
| Identity key protection | Signing only through `IdentityManager.sign()`. The private key is never in protocol code. |
| Key material | Ephemeral and session keys are in-memory `ByteArray`s, zero-filled on close, never logged |
| Revoked Mac | `observeTrustedDevices()` stops listing it → close 4003 and wipe (§8.4) |
| Sensitive clips | `EXTRA_IS_SENSITIVE` clips are never captured [SPEC §9.5] |
| Malicious payload | Strict parsing and validation [SPEC §10.4]. Text is never executed, rendered as HTML or auto-opened. Clipboard text is never logged (the `AppLog` rule extended). |
| Oversized input | Size guard before decryption (§3.1) |
| Cleartext transport | Allowed only because the channel provides confidentiality and integrity [SPEC §4.1]. The build-time guard test limits cleartext URLs to the sync endpoint. |
| Fingerprint comparison | The Identity screen shows this phone's key fingerprint, computed as in [SPEC §12.3], so the user can match it against the Mac's "Allow Clipboard Sync" screen. |

---

## 12. Architecture [ANDROID]

### 12.1 Modules

```
:core:syncprotocol  (JVM)      Sync Channel v1 codec and crypto (SPEC §3.3, §5–§9, §12.3). Depends on :core:protocol
                               (read-only use of HandshakeCrypto, Tlv, StrictTlvReader, TlvBuilder, byte utilities).
:core:sync          (JVM)      Sync domain: engine, ordering, dedupe, outbox, connection state machine, ports.
                               Depends on :core:syncprotocol.
:core:lan           (Android)  OkHttp WebSocket transport, NSD resolver, Wi‑Fi monitor. Implements :core:sync ports.
:core:clipboard     (Android)  ClipboardManager adapter. Implements :core:sync ports.
:core:syncstore     (Android)  Room connectflow_sync.db, sealed content via :core:security KeystoreManager.
                               Implements :core:sync ports.
:feature:clipboard  (Android)  Clipboard screen, history screen, ViewModels.
:app                           ClipboardSyncService, SendClipboardActivity, ShareToMacActivity,
                               SendClipboardTileService, SyncNotifications, di/SyncModule, sync adapters.
```

- **Dependency direction:**
  - `:core:syncprotocol` → `:core:protocol`
  - `:core:sync` → `:core:syncprotocol`
  - The Android modules → `:core:sync`
  - `:app` wires everything.
- **Not changed:** `:core:protocol`, `:core:pairing`, `:core:networking`, `:core:identity`, `:core:security`, `:core:trusteddevices`.
- **Legacy `:feature:settings`:** stays unused.

### 12.2 `:core:syncprotocol`

| Type | Responsibility |
|---|---|
| `SyncChannelConstants` | Every constant of [SPEC Appendix A]. A unit test cross-checks them against the committed vectors. |
| `SyncTranscript` | `H`, `T`, `TH`, `authInput()` [SPEC §6.1] |
| `ClientHello`, `ServerHello`, `ClientAuth` | Encode and strict decode [SPEC §6.2–§6.4] |
| `ChannelKeySchedule` | `k_a2m` / `k_m2a` [SPEC §6.5], using `HandshakeCrypto.hkdfSha256` |
| `RecordCipher` | AES‑256‑GCM records [SPEC §7], using `javax.crypto` |
| `ClientChannelHandshake` | The initiator steps and checks [SPEC §6.6], producing a `SecureSession` |
| `SecureSession` | Directional keys and sequence counters [SPEC §7.2]; `seal`, `open`, `wipe` |
| `AppMessage` (`Ready`, `Clip`, `Ack`) and `AppMessageCodec` | [SPEC §8] |
| `ClipContentRules` | [SPEC §9.2–§9.4] |
| `Idh` / `KeyFingerprint` | [SPEC §3.3] / [SPEC §12.3] |
| `testFixtures`: `MacChannelResponder` | A reference Mac for tests, following the Mac steps of [SPEC §6.6] |

Interfaces used inside the protocol code:

```kotlin
// Transport-neutral binary message pipe for one connection.
interface ChannelLink {
    suspend fun send(message: ByteArray)
    suspend fun receive(timeoutMillis: Long): ByteArray   // throws ChannelClosedException
    fun close(code: Int)
}

// Identity material for one handshake. The private key stays behind sign().
class ChannelIdentity(val deviceId: ByteArray, val publicKey: ByteArray, val signer: IdentitySigner)
class ChannelPeer(val deviceId: ByteArray, val publicKey: ByteArray)   // the pinned Mac
```

### 12.3 `:core:sync`

| Type | Responsibility |
|---|---|
| `ClipItem`, `ClipKind`, `ClipDirection`, `SyncStatus` | Domain models |
| `SyncEngine` | Capture flow (§5.5), receive order [SPEC §10.4], sending rules [SPEC §10.2] |
| `ClipOrdering` | [SPEC §10.3] |
| `Outbox` | [SPEC §11] |
| `ConnectionManager` | State machine (§3.3), reconnection [SPEC §14.2] |
| `SyncPolicy` | The timing values of [SPEC §13] and the limits of [SPEC §4.3]. They are not independent Android values. |

Ports (implemented by the Android modules):

```kotlin
interface ActivePeerSource {            // :app adapter over TrustedDeviceRepository + sync_state
    fun observeActivePeer(): Flow<SyncPeer?>          // null when none, revoked or sync disabled
}
interface LocalChannelIdentitySource {  // :app adapter over IdentityManager
    suspend fun load(): ChannelIdentity
}
interface EndpointResolver {            // :core:lan
    fun resolve(peer: SyncPeer): Flow<Endpoint>       // fast-path candidates, then NSD results
}
interface ChannelConnector {            // :core:lan
    suspend fun open(endpoint: Endpoint): ChannelLink
}
interface NetworkMonitor {              // :core:lan
    val state: StateFlow<NetworkState>                // WaitingForNetwork / Available(network, ipv4)
}
interface ClipboardSink {               // :core:clipboard
    suspend fun write(text: String)
}
interface SyncStateStore {              // :core:syncstore
    suspend fun load(): SyncState
    suspend fun save(state: SyncState)
    suspend fun markSeen(peerId: String, messageId: ByteArray): Boolean   // false if already seen
    suspend fun saveEndpoint(peerId: String, endpoint: Endpoint)
}
interface HistoryStore {                // :core:syncstore
    fun observe(): Flow<List<HistoryEntry>>
    suspend fun record(item: ClipItem, direction: ClipDirection, peerId: String)
    suspend fun clear()
}
interface PeerSeenRecorder {            // :app adapter over TrustedDeviceRepository.recordSeen
    suspend fun seen(peerId: String, host: String)
}
interface SyncClock { fun nowMillis(): Long; fun elapsedMillis(): Long }
```

Capture sources (`:core:clipboard`, `:app`) call `SyncEngine.submit(text, explicit: Boolean)`. Clipboard *reading* isn't a port, because it happens only in focused Android components.

### 12.4 Android modules

| Module | Classes |
|---|---|
| `:core:lan` | `OkHttpChannelConnector` (subprotocol and extension checks, `pingInterval`, Wi‑Fi `socketFactory`, size guard), `NsdEndpointResolver`, `FastPathCandidates`, `WifiNetworkMonitor` |
| `:core:clipboard` | `ClipboardReader` (focused read, sensitive check, item 0 `getText()`), `AndroidClipboardSink` (`setPrimaryClip` on main), `InAppClipboardListener` (registered by `MainActivity`) |
| `:core:syncstore` | `SyncDatabase`, `ClipHistoryDao`, `SeenMessageDao`, `SyncStateDao`, `PeerEndpointDao`, `SealedContent` (`KeystoreManager` wrapper), `RoomSyncStateStore`, `RoomHistoryStore`, `RetentionSweeper` |
| `:feature:clipboard` | `ClipboardSyncScreen` / `ClipboardSyncViewModel` (Mac selector, sync toggle, status, "Send clipboard", battery guide), `ClipboardHistoryScreen` / `ClipboardHistoryViewModel` (list, copy, open URL, delete, clear, keep-history toggle) |
| `:app` | `ClipboardSyncService`, `SyncNotifications`, `SendClipboardActivity`, `ShareToMacActivity`, `SendClipboardTileService`, `di/SyncModule`, `sync/SyncAdapters.kt`, and the fingerprint row on the existing Identity screen (§11) |

### 12.5 Manifest and resources

- **Permissions:** `INTERNET`, `CHANGE_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `POST_NOTIFICATIONS`. `ACCESS_NETWORK_STATE` is already declared.
- **Components:**
  - `<service ClipboardSyncService foregroundServiceType="connectedDevice" exported="false">`
  - `<service SendClipboardTileService permission="android.permission.BIND_QUICK_SETTINGS_TILE" exported="true">`, with the `QS_TILE` intent filter
  - `<activity SendClipboardActivity exported="false">`
  - `<activity ShareToMacActivity exported="true">`, with an `ACTION_SEND` `text/plain` filter
- **Resources:**
  - `res/xml/network_security_config.xml`: `base-config cleartextTrafficPermitted="true"` (§3.1)
  - `data_extraction_rules.xml`: three new excludes (§10.1)

### 12.6 Dependencies

| Dependency | Scope | Why |
|---|---|---|
| `com.squareup.okhttp3:okhttp:4.12.0` | `:core:lan` | WebSocket client. The 4.x line is compatible with Kotlin 1.9.24. |
| `com.squareup.okhttp3:mockwebserver:4.12.0` | test | WebSocket server for integration tests |
| `org.robolectric:robolectric` (pinned at implementation time) | test | `NsdManager` and `ClipboardManager` shadows |
| `app.cash.turbine` (already in the catalog, unused) | test | Flow assertions |

- No serialization library (TLV is reused) and no new crypto library.

---

## 13. Testing [ANDROID, plus SPEC §15 conformance]

Tests marked ★ must exist, and pass, before the feature is enabled for users.

### 13.1 Spec conformance (`:core:syncprotocol`, JVM)

- ★ `SyncChannelVectorsTest`:
  - loads the committed `ecosystem-shared-docs/protocol/test-vectors/sync-channel-v1-vectors.json`
  - regenerates **every** value from its `inputs` byte for byte, including `sig_m` and `sig_a`, with the Android code paths (`HandshakeCrypto`, `IdentityManager`-equivalent Tink signing, `javax.crypto`)
  - meets every `negative` expectation [SPEC §15.3]
  - Android does not generate or edit the vector file.
- ★ `SyncChannelConstantsTest`: every value in `SyncChannelConstants` equals [SPEC Appendix A] and the vectors.
- ★ `SyncHandshakeTest` against `MacChannelResponder`:
  - every Android-side check and close code of [SPEC §6.6]
  - a replayed S1 from an earlier session
  - timeouts at each step
- ★ `RecordCipherTest` and `AppMessageCodecTest`: [SPEC §7, §8], including the minimum record size, unknown `appType`, a long `READY`, an unknown `ACK` result, and an unmatched `ACK` being ignored.

### 13.2 Unit tests (JVM)

- ★ `ClipContentRulesTest`: [SPEC §9.2–§9.4] boundaries, plus the Android UTF-16 → UTF-8 surrogate case.
- ★ `ClipOrderingTest`: [SPEC §10.3] including an equal tuple, a null current state and the unsigned tie-break.
- ★ `SyncEngineTest` with fakes: each step of [SPEC §10.4], echo suppression, never relaying, the explicit-send bypass, history writes, every `ACK` result, and `NOT_ACCEPTED` handling.
- ★ `OutboxTest` with a fake clock: [SPEC §11]. Latest-only replacement, TTL boundaries, byte-identical retry, a late `ACK` for a replaced item ignored.
- ★ `ConnectionManagerTest` with a fake clock and network: §3.3 transitions, [SPEC §14.2] backoff bounds and reset, immediate triggers, `Blocked` for 4001, 4003 and 4007.

### 13.3 WebSocket tests (`:core:lan`, JVM with MockWebServer)

- ★ Subprotocol missing or wrong → 1002. `Sec-WebSocket-Extensions` present → 1002. A text frame → 1003 [SPEC §4.1, §4.2].
- ★ Full handshake and `CLIP`/`ACK` round trip against a MockWebServer running `MacChannelResponder`.
- ★ An oversized incoming message → 1009 without decryption [SPEC §4.3].
- ★ A missed pong fails the connection. Server silence during the handshake → 4005 [SPEC §13].
- Close code → retry policy [SPEC §14.2].

### 13.4 Discovery tests

- `NsdEndpointResolverTest` (Robolectric or a fake `NsdManager` wrapper):
  - TXT rules [SPEC §3.2]
  - IPv6-only services skipped
  - serial resolve queue on API < 34
  - the `ServiceInfoCallback` path on API 34+
  - service lost and updated
- `FastPathCandidatesTest`: [SPEC §3.5] order, IPv4 filtering, connect timeouts.
- ★ `IdhTest` and `KeyFingerprintTest`: equal to the vectors.

### 13.5 Reconnect and offline tests

- ★ Engine-level scenarios with fakes:
  - Wi‑Fi lost, then regained
  - IP change mid-session
  - Mac unreachable for 5 minutes
  - drop before `ACK` → byte-identical resend → receiver `DUPLICATE`
  - pending item expires
  - app restart restores the state
  - trust revoked while connected → 4003 and wipe
  - the Mac closes with 4003 → `Blocked`
- `WifiNetworkMonitorTest`: callback mapping, IPv4 extraction, socket-factory binding.

### 13.6 Clipboard and storage tests

- Instrumented, on API 29 and API 36 emulators:
  - ★ a focused read succeeds
  - an unfocused read returns null
  - `SendClipboardActivity` reads only after focus
  - sensitive clip skipped (API 33+)
  - URI-only and HTML clips
  - `setPrimaryClip` from the running service while the app is in the background
  - echo suppression when the listener fires after our own write
- Robolectric: `ClipboardReader` / `AndroidClipboardSink` mapping.
- Room: ★ `SyncDatabase` schema export, retention [SPEC §16], sealed content round trip, associated-data mismatch detection, clear history, revoke cascade, backup exclusion of the three database files.

### 13.7 Physical-device tests (Samsung SM‑S928B, Android 16 ↔ Mac)

| ID | Test |
|---|---|
| P‑1 | First connection via NSD. The fast path works on the second launch. |
| P‑2 | Android → Mac via each trigger: notification, tile, in-app button, share sheet |
| P‑3 | Mac → Android with ConnectFlow in the background and the screen off |
| P‑4 | Loop check: copy on each side 20 times in alternation. No echoes, one history entry per copy. |
| P‑5 | Concurrent copies on both sides within 1 s. Both clipboards end equal. |
| P‑6 | The largest allowed text succeeds, one byte more is refused with a message, a URL is received as text and not opened |
| P‑7 | Revoke the Mac on Android, and turn off "Allow Clipboard Sync" on the Mac, while connected. Both lead to 4003, keys wiped, pending item removed, `Blocked` status on Android. |
| P‑8 | Rogue server on the same LAN advertising `_connectflow._tcp` with the correct `idh`. Android refuses it (4001) and keeps looking. |
| P‑9 | The fingerprint on Android's Identity screen matches the one on the Mac's "Allow Clipboard Sync" screen |

### 13.8 Doze and background tests

| ID | Test |
|---|---|
| D‑1 | `adb shell dumpsys deviceidle force-idle`, then a Mac → Android item. Expected: delivered, or reconnected within 30 s after `unforce`. |
| D‑2 | `adb shell am set-inactive com.ecosystem.android true` (app standby). Same expectation. |
| D‑3 | 8-hour overnight soak with the screen off: reconnect count, battery drain, final state `Connected` |
| D‑4 | One UI "Put app to sleep" / "Deep sleeping": document the behaviour and verify the guidance screen |
| D‑5 | `am kill` while sync is on: sticky restart and reconnection |
| D‑6 | Android 16 local-network protection enabled for the app: record what fails and what permission is needed |
| B‑4 | Foreground-service restart after a system kill on One UI |

### 13.9 Wi‑Fi switching tests

| ID | Test |
|---|---|
| W‑1 | `adb shell svc wifi disable` / `enable`: `WaitingForNetwork` → reconnect without backoff |
| W‑2 | Move the phone between two SSIDs: new discovery, no session reuse, no traffic over cellular |
| W‑3 | Wi‑Fi without internet: the sync socket stays on Wi‑Fi through the bound socket factory |
| W‑4 | DHCP renew with a new phone IP: reconnect at once |
| W‑5 | The Mac switches from Wi‑Fi to Ethernet (new IP): rediscovery within the backoff window |
| W‑6 | Guest network with client isolation or no multicast: "Mac not reachable", no crash. Documented limitation. |
| W‑7 | Airplane mode on and off during an `ACK` wait: byte-identical resend, receiver `DUPLICATE` |

---

## 14. Outcome

### 14.1 Android decisions

1. **Discovery mechanics:** the `NsdManager` API split, resolve queue, endpoint cache and `recordSeen()` (§2).
2. **Transport mechanics:** OkHttp configuration and checks, Wi‑Fi socket binding, the cleartext network security config with its build guard, the state machine, reconnect triggers and status texts (§3).
3. **Crypto mapping:** key sources and primitives, AES‑GCM through `javax.crypto`, key wiping, and never sending `NOT_ACCEPTED` in Sprint 2 (§4).
4. **Capture UX:** focused capture only, no read on app open, `SendClipboardActivity`, the Quick Settings tile, the share sheet (§5).
5. **Apply behaviour:** main-thread `setPrimaryClip` and the feedback rules (§6).
6. **Loop-state storage:** where `L`, `C` and the seen set live, and the unsigned-byte handling (§7).
7. **Offline mechanics:** network monitoring, the outbox clock, and the situation handling (§8).
8. **Background execution:** the `connectedDevice` foreground service, user-visible starts, `START_STICKY`, no boot start, no Companion Device Manager or WorkManager, and the battery guide (§9).
9. **Storage:** `connectflow_sync.db`, its schema, Keystore sealing, retention mechanics and backup exclusions (§10).
10. **Security and structure:** Android's security measures and fingerprint display (§11), the modules, classes, ports, manifest and dependencies (§12), and the Android test plan (§13).

### 14.2 Cross-platform decisions

- **All defined by the spec:** every cross-platform decision is defined by `SYNC_CHANNEL_V1.md` (committed at `f12dc3f`), and this document no longer holds any pending cross-platform proposal.
- **How to change one:** a change to any of them goes through the spec, as a new channel version with new vectors [SPEC §18].

### 14.3 What macOS implements

macOS implements `SYNC_CHANNEL_V1.md`, meeting its conformance requirements [SPEC §15.3]. This document places no further requirement on macOS.

---

## Appendix A — Changes from revision 1

Revision 1 of this document proposed the cross-platform contract. Those parts now live in `SYNC_CHANNEL_V1.md`, and this revision references them instead of restating them. These points of revision 1 disagreed with, or were missing from, the committed spec, and are corrected here:

| # | Revision 1 | Now (per spec) |
|---|---|---|
| 1 | Receiver re-checked `kind` only when it was URL | `kind` must equal the classification in either direction [SPEC §9.3, §10.4] |
| 2 | Equal `(lamport, origin)` tuple not defined | `DUPLICATE` [SPEC §10.4 step 9] |
| 3 | No rule for an empty current state | An incoming item always wins [SPEC §10.3] |
| 4 | No range rule for `lamport` / `createdAt` | Both must be below 2^63 [SPEC §8.3] |
| 5 | `NOT_ACCEPTED` defined without a processing position or Android use | Step 8 of [SPEC §10.4]. Android never sends it in Sprint 2 (§4.4). |
| 6 | The Mac rejects C1 with "4001 or 4003" | Unknown device → 4001; revoked or switch off → 4003 [SPEC §6.6] |
| 7 | Trust gap: either close Pairing §13.1 first, or a Mac switch with Mac → Android off by default | A per-device "Allow Clipboard Sync" switch, **off by default for all sync**, plus a fingerprint display. §13.1 remains a future pairing version. [SPEC §12.2, §17.3] |
| 8 | "Key fingerprint" without a definition | Formula in [SPEC §12.3]. Android displays its own (§11). |
| 9 | Mac sensitive markers: Concealed and Transient | Also AutoGenerated [SPEC §9.5] |
| 10 | The 24 h session limit was Android-only | Both sides enforce it [SPEC §4.4] |
| 11 | Mac upgrade handling not specified | HTTP 404 / 400, 1002 fallback, 5 s upgrade limit [SPEC §4.1, §6.6] |
| 12 | No minimum record size | Shorter than 27 bytes → 4002 [SPEC §7.1] |
| 13 | `READY` misuse, unknown `ACK` results and unmatched `ACK`s not specified | 4002, 4002, ignored [SPEC §8.2, §8.4] |
| 14 | Outbox replacement and retry content not fully specified | A replaced item's late `ACK` is ignored, and retries are byte-identical [SPEC §11] |
| 15 | 4003 status text "Sync turned off on the Mac" | "Clipboard sync is off on the Mac" [SPEC §12.4] |
| 16 | The Android test would *generate* the vector file | The vectors are committed. Android's test must *regenerate* them byte for byte and meet the negative cases [SPEC §15.3]. |
| 17 | Unknown TXT keys not addressed | Ignored [SPEC §3.2] |
| 18 | Only Android pings | The Mac MAY also ping. OkHttp answers automatically. [SPEC §4.2] |
| 19 | Status: proposals pending macOS agreement; §14.2 / §14.3 lists of open items | Resolved; the spec is the authority |
| 20 | Byte layouts, labels, tags, limits and codes restated as Android decisions | Replaced by [SPEC] references (§2–§8, §12, §13) |

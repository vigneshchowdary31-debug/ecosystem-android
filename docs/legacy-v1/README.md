# Legacy v1 protocol documents (superseded)

These documents describe the v1 pairing protocol (`identity_continuity:` CSV QR code,
colon-delimited `auth:` / `auth_resp:` strings, empty HKDF info). v1 is insecure: its
signatures were not bound to the key exchange, so replay and man-in-the-middle attacks were
possible. They also contradict each other in places.

They are kept for history only. The normative contract is
`ecosystem-shared-docs/protocol/PAIRING_PROTOCOL_V2.md`.

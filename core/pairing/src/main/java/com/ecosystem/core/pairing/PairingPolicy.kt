package com.ecosystem.core.pairing

import com.ecosystem.core.protocol.handshake.HandshakeTimeouts

/** Timeouts and retry limits for one pairing session. Defaults are documented in the protocol spec. */
data class PairingPolicy(
    /** Hard limit for the whole session, across every candidate. */
    val overallTimeoutMillis: Long = 60_000,
    val scanTimeoutMillis: Long = 12_000,
    /** Extra scan time after the first result, to see other nearby candidates. */
    val scanSettleMillis: Long = 1_500,
    /** Attempts per candidate. Only transient failures (such as GATT 133) are retried. */
    val maxAttemptsPerCandidate: Int = 2,
    val maxTotalAttempts: Int = 8,
    val retryDelayMillis: Long = 750,
    val handshakeTimeouts: HandshakeTimeouts = HandshakeTimeouts(),
)

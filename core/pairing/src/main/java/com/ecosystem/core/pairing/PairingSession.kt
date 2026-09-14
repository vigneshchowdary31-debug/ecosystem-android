package com.ecosystem.core.pairing

import com.ecosystem.core.protocol.handshake.HandshakeResult
import com.ecosystem.core.protocol.handshake.InitiatorHandshake
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One cryptographic pairing attempt against one candidate. It owns the connection and the
 * handshake (which owns the ephemeral key, nonces, derived key and transcript). Nothing here
 * outlives the attempt: [close] wipes the secrets and closes the connection, and it runs on
 * success, failure and cancellation alike.
 */
internal class PairingSession(
    val id: String,
    val candidate: PairingCandidate,
    private val transport: PairingTransport,
    private val handshake: InitiatorHandshake,
) {
    private val closed = AtomicBoolean(false)

    @Volatile
    private var connection: PairingConnection? = null

    suspend fun run(onPhase: (ConnectionPhase) -> Unit): HandshakeResult {
        check(!closed.get()) { "Session $id already closed" }
        val opened = transport.connect(candidate, onPhase)
        connection = opened
        if (closed.get()) {
            opened.close()
            throw CancellationException("Session $id closed while connecting")
        }
        return handshake.run(opened)
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            handshake.wipe()
            connection?.close()
            connection = null
        }
    }
}

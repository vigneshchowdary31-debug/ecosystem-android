package com.ecosystem.core.protocol.link

import com.ecosystem.core.protocol.framing.Frame

/** A bidirectional, ordered stream of frames between the two pairing peers. */
interface MessageChannel {
    /** Sends one frame and returns once the transport has accepted every fragment. */
    suspend fun send(frame: Frame)

    /** Waits up to [timeoutMillis] for the next complete frame. Throws [LinkException] on failure. */
    suspend fun receive(timeoutMillis: Long): Frame
}

class LinkException(val kind: Kind, message: String, cause: Throwable? = null) : Exception(message, cause) {
    enum class Kind { TIMEOUT, DISCONNECTED, WRITE_FAILED, FRAMING_ERROR, PERMISSION_DENIED }
}

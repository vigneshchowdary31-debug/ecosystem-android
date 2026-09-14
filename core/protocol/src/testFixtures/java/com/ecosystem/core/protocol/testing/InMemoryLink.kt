package com.ecosystem.core.protocol.testing

import com.ecosystem.core.protocol.framing.Frame
import com.ecosystem.core.protocol.framing.FrameCodec
import com.ecosystem.core.protocol.framing.FrameReassembler
import com.ecosystem.core.protocol.framing.Fragmenter
import com.ecosystem.core.protocol.framing.FramingException
import com.ecosystem.core.protocol.link.LinkException
import com.ecosystem.core.protocol.link.MessageChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Byte-level in-memory link for tests. Frames are encoded, cut into packets of at most
 * [chunkSize] bytes (like BLE writes/notifications) and reassembled on the other side, so
 * every test that uses it also exercises the framing layer.
 */
object InMemoryLink {
    /** Returns (androidEnd, macEnd). */
    fun pair(chunkSize: Int = 20): Pair<InMemoryEndpoint, InMemoryEndpoint> {
        val android = InMemoryEndpoint("android", chunkSize)
        val mac = InMemoryEndpoint("mac", chunkSize)
        android.peer = mac
        mac.peer = android
        return android to mac
    }
}

class InMemoryEndpoint internal constructor(val name: String, private val chunkSize: Int) : MessageChannel {
    internal lateinit var peer: InMemoryEndpoint
    private val incoming = Channel<Frame>(Channel.UNLIMITED)
    private val reassembler = FrameReassembler()
    private val lock = Any()

    /** Rewrites or drops (by returning null) each outgoing frame. Lets tests act as a man in the middle. */
    @Volatile var frameInterceptor: (Frame) -> Frame? = { it }

    /** Rewrites the encoded bytes of each outgoing frame, e.g. to corrupt the header. */
    @Volatile var byteInterceptor: ((ByteArray) -> ByteArray)? = null

    val sentFrames = CopyOnWriteArrayList<Frame>()
    val sentPacketSizes = CopyOnWriteArrayList<Int>()

    @Volatile var isClosed = false
        private set

    override suspend fun send(frame: Frame) {
        if (isClosed) throw LinkException(LinkException.Kind.DISCONNECTED, "$name is closed")
        val outgoing = frameInterceptor(frame) ?: return
        sentFrames += outgoing
        var bytes = FrameCodec.encode(outgoing)
        byteInterceptor?.let { bytes = it(bytes) }
        for (packet in Fragmenter.split(bytes, chunkSize)) {
            sentPacketSizes += packet.size
            peer.deliver(packet)
        }
    }

    /** Injects raw bytes as if they had arrived over the air. */
    fun deliver(packet: ByteArray) {
        synchronized(lock) {
            try {
                reassembler.append(packet).forEach { incoming.trySend(it) }
            } catch (e: FramingException) {
                incoming.close(LinkException(LinkException.Kind.FRAMING_ERROR, e.message ?: "framing error", e))
            }
        }
    }

    override suspend fun receive(timeoutMillis: Long): Frame {
        val result = withTimeoutOrNull(timeoutMillis) { incoming.receiveCatching() }
            ?: throw LinkException(LinkException.Kind.TIMEOUT, "$name timed out after $timeoutMillis ms")
        if (result.isSuccess) return result.getOrThrow()
        val cause = result.exceptionOrNull()
        throw cause as? LinkException ?: LinkException(LinkException.Kind.DISCONNECTED, "$name channel closed", cause)
    }

    /** Closes both directions, as a BLE disconnect would. */
    fun disconnect() {
        isClosed = true
        incoming.close(LinkException(LinkException.Kind.DISCONNECTED, "$name disconnected"))
        peer.onPeerDisconnected()
    }

    private fun onPeerDisconnected() {
        isClosed = true
        incoming.close(LinkException(LinkException.Kind.DISCONNECTED, "peer of $name disconnected"))
    }
}

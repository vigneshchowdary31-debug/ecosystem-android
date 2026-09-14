package com.ecosystem.core.protocol.testing

import java.security.SecureRandom

/** Returns pre-scripted byte arrays, in order, from [nextBytes]. Only for deterministic test vectors. */
class ScriptedSecureRandom(values: List<ByteArray>) : SecureRandom() {
    private val queue = ArrayDeque(values.map { it.copyOf() })

    override fun nextBytes(bytes: ByteArray) {
        val next = queue.removeFirstOrNull() ?: error("ScriptedSecureRandom exhausted")
        require(next.size == bytes.size) { "Scripted value has ${next.size} bytes, ${bytes.size} requested" }
        next.copyInto(bytes)
    }
}

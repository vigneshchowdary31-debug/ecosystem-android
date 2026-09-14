package com.ecosystem.core.security

/** In-memory [SecureStorage] for JVM tests. Values can be marked legacy or unreadable to simulate old installs and lost keys. */
class FakeSecureStorage : SecureStorage {
    val values = LinkedHashMap<String, SecureRead>()

    override fun putBytes(key: String, value: ByteArray) {
        values[key] = SecureRead.Present(value.copyOf(), isLegacyFormat = false)
    }

    override fun readBytes(key: String): SecureRead = when (val read = values[key]) {
        null -> SecureRead.Missing
        is SecureRead.Present -> SecureRead.Present(read.value.copyOf(), read.isLegacyFormat)
        else -> read
    }

    override fun putString(key: String, value: String) = putBytes(key, value.toByteArray(Charsets.UTF_8))

    override fun getString(key: String): String? =
        (readBytes(key) as? SecureRead.Present)?.let { String(it.value, Charsets.UTF_8) }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun clear() = values.clear()
}

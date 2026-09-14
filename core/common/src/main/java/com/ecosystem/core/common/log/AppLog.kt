package com.ecosystem.core.common.log

import android.util.Log

/**
 * The app's only logging entry point. Disabled by default and switched on only for
 * debuggable builds (see `CompanionApp`), so release builds emit nothing.
 *
 * Never pass key material, session keys, signatures, raw handshake payloads or full QR
 * contents to this logger.
 */
object AppLog {
    @Volatile
    var isEnabled: Boolean = false

    fun d(tag: String, message: () -> String) {
        if (isEnabled) Log.d(tag, message())
    }

    fun d(tag: String, message: String) {
        if (isEnabled) Log.d(tag, message)
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        if (isEnabled) Log.w(tag, message, error)
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        if (isEnabled) Log.e(tag, message, error)
    }
}

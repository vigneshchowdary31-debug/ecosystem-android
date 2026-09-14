package com.ecosystem.android

import androidx.compose.runtime.Composable

/** Release builds ship no developer tools. */
object DebugTools {
    @Composable
    fun tabs(): List<DebugTab> = emptyList()
}

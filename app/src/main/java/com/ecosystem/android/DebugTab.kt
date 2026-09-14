package com.ecosystem.android

import androidx.compose.runtime.Composable

/** A developer tool shown as an extra tab. Only debug builds provide any (see `src/debug/.../DebugTools.kt`). */
class DebugTab(val title: String, val content: @Composable () -> Unit)

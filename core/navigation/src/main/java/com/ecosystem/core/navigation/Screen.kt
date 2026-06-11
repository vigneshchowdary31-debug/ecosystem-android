package com.ecosystem.core.navigation

sealed class Screen(val route: String) {
    object Pairing : Screen("pairing")
    object Settings : Screen("settings")
}

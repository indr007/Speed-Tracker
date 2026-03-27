package com.example.speedtracker.ui.navigation

sealed class Screen(val route: String) {
    object Speedometer : Screen("main")
    object History : Screen("history")
    object Maps : Screen("map?tripId={tripId}") {
        fun createRoute(tripId: Long? = null) = if (tripId != null) "map?tripId=$tripId" else "map"
    }
    object Paywall : Screen("paywall")
    object Settings : Screen("settings")
}

package com.example.speedtracker.utils

import java.util.Locale

object SpeedConverter {
    /**
     * Converts speed from meters/second to kilometers/hour.
     */
    fun msToKmh(ms: Float): Double {
        return (ms * 3.6)
    }

    /**
     * Formats speed to 1 decimal place.
     */
    fun formatSpeed(speedKmh: Double): String {
        return String.format(Locale.getDefault(), "%.1f", speedKmh)
    }

    /**
     * Converts meters to kilometers.
     */
    fun mToKm(m: Float): Double {
        return (m / 1000.0)
    }
}

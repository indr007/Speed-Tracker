package com.example.speedtracker.utils

object Constants {
    const val NOTIFICATION_CHANNEL_ID = "speed_tracker_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Speed Tracking"
    const val NOTIFICATION_ID = 1

    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

    const val LOCATION_UPDATE_INTERVAL = 2000L
    const val FASTEST_LOCATION_INTERVAL = 1000L

    const val SHARED_PREFERENCES_NAME = "speedTrackerPrefs"
    const val KEY_SPEED_LIMIT = "KEY_SPEED_LIMIT"
}

package com.example.speedtracker.data.model

import androidx.room.*

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val maxSpeed: Double,
    val avgSpeed: Double,
    val distance: Double,
    val tourPath: String // JSON string of coordinates
)

package com.example.speedtracker.data.repository

import com.example.speedtracker.data.local.TripDao
import com.example.speedtracker.data.model.Trip

class TripRepository(private val tripDao: TripDao) {
    val allTrips = tripDao.getAllTrips()

    suspend fun insert(trip: Trip) = tripDao.insertTrip(trip)
    suspend fun delete(trip: Trip) = tripDao.deleteTrip(trip)
    suspend fun getTripById(tripId: Long) = tripDao.getTripById(tripId)
    suspend fun clearHistory() = tripDao.clearHistory()
}

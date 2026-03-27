package com.example.speedtracker.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.speedtracker.data.model.Trip

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): LiveData<List<Trip>>

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): Trip?

    @Query("DELETE FROM trips")
    suspend fun clearHistory()
}

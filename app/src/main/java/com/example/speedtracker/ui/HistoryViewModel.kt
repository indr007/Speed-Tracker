package com.example.speedtracker.ui

import androidx.lifecycle.*
import com.example.speedtracker.data.model.Trip
import com.example.speedtracker.data.repository.TripRepository
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: TripRepository) : ViewModel() {
    val allTrips = repository.allTrips

    fun deleteTrip(trip: Trip) = viewModelScope.launch {
        repository.delete(trip)
    }

    suspend fun getTripById(tripId: Long): Trip? {
        return repository.getTripById(tripId)
    }

    fun clearHistory() = viewModelScope.launch {
        repository.clearHistory()
    }
}

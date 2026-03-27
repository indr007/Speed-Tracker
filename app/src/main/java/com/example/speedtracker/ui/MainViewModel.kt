package com.example.speedtracker.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import com.example.speedtracker.data.model.Trip
import com.example.speedtracker.data.repository.TripRepository
import com.example.speedtracker.data.subscription.SubscriptionStatusManager
import com.example.speedtracker.logic.LocationService
import com.example.speedtracker.utils.Constants
import com.example.speedtracker.utils.SpeedConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(
    application: Application,
    private val repository: TripRepository,
    private val statusManager: SubscriptionStatusManager
) : AndroidViewModel(application) {

    // 🔥 RAW DATA from Service
    val isTracking = LocationService.isTracking
    val currentSpeedRaw = LocationService.currentSpeed
    val distanceTraveledRaw = LocationService.distanceTraveled

    // 🔥 Premium Status
    val isPremium: StateFlow<Boolean> = statusManager.isSubscribed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 🔥 FIXED LiveData (no Transformations)
    val currentSpeedKmh = MutableLiveData<Double>()
    val distanceKm = MutableLiveData<Double>()

    private val _maxSpeedKmh = MutableLiveData(0.0)
    val maxSpeedKmh: LiveData<Double> = _maxSpeedKmh

    private val _avgSpeedKmh = MutableLiveData(0.0)
    val avgSpeedKmh: LiveData<Double> = _avgSpeedKmh

    // 🔥 Speed Alert System (Premium controllable)
    private val _speedThreshold = MutableLiveData(80.0)
    val speedThreshold: LiveData<Double> = _speedThreshold

    private var lastAlertTime = 0L
    private val alertCooldownMs = 30000L // 30 seconds

    private val _showAlert = MutableLiveData(false)
    val showAlert: LiveData<Boolean> = _showAlert

    private val _safetyMessage = MutableLiveData("")
    val safetyMessage: LiveData<String> = _safetyMessage

    // 🔥 UI Customization (Premium feature)
    private val _speedometerTheme = MutableLiveData("Standard")
    val speedometerTheme: LiveData<String> = _speedometerTheme

    private var startTime: Long = 0

    init {
        // 🔥 Prefill from SharedPreferences if possible
        val prefs = application.getSharedPreferences("premium_settings", Context.MODE_PRIVATE)
        _speedThreshold.value = prefs.getFloat("speed_threshold", 80f).toDouble()
        _speedometerTheme.value = prefs.getString("speedometer_theme", "Standard")

        // 🔥 observe speed safely
        currentSpeedRaw.observeForever { speed ->
            val speedKmh = SpeedConverter.msToKmh(speed ?: 0f)
            updateStats(speedKmh)
            currentSpeedKmh.postValue(speedKmh)
        }

        // 🔥 observe distance safely
        distanceTraveledRaw.observeForever { distance ->
            val km = SpeedConverter.mToKm(distance ?: 0f)
            distanceKm.postValue(km)
        }
    }

    fun setSpeedThreshold(newThreshold: Double) {
        if (isPremium.value) {
            _speedThreshold.value = newThreshold
            val prefs = getApplication<Application>().getSharedPreferences("premium_settings", Context.MODE_PRIVATE)
            prefs.edit().putFloat("speed_threshold", newThreshold.toFloat()).apply()
        }
    }

    fun setSpeedometerTheme(theme: String) {
        if (isPremium.value) {
            _speedometerTheme.value = theme
            val prefs = getApplication<Application>().getSharedPreferences("premium_settings", Context.MODE_PRIVATE)
            prefs.edit().putString("speedometer_theme", theme).apply()
        }
    }

    fun startTrip() {
        startTime = System.currentTimeMillis()
        _maxSpeedKmh.value = 0.0
        _avgSpeedKmh.value = 0.0

        val intent = Intent(getApplication(), LocationService::class.java).apply {
            action = Constants.ACTION_START_OR_RESUME_SERVICE
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun stopTrip() {
        val endTime = System.currentTimeMillis()

        val pathPoints = LocationService.pathPoints.value ?: mutableListOf()
        val pathJson = JSONArray().apply {
            pathPoints.forEach { point ->
                put(JSONObject().apply {
                    put("latitude", point.latitude)
                    put("longitude", point.longitude)
                })
            }
        }.toString()

        val trip = Trip(
            startTime = startTime,
            endTime = endTime,
            maxSpeed = _maxSpeedKmh.value ?: 0.0,
            avgSpeed = _avgSpeedKmh.value ?: 0.0,
            distance = distanceKm.value ?: 0.0,
            tourPath = pathJson
        )

        viewModelScope.launch {
            repository.insert(trip)
        }

        val intent = Intent(getApplication(), LocationService::class.java).apply {
            action = Constants.ACTION_STOP_SERVICE
        }
        getApplication<Application>().startService(intent)
    }

    fun updateStats(speedKmh: Double) {
        if (speedKmh > (_maxSpeedKmh.value ?: 0.0)) {
            _maxSpeedKmh.postValue(speedKmh)
        }

        val currentAvg = _avgSpeedKmh.value ?: 0.0
        if (currentAvg == 0.0) {
            _avgSpeedKmh.postValue(speedKmh)
        } else {
            _avgSpeedKmh.postValue((currentAvg + speedKmh) / 2)
        }

        // 🔥 Check for Speed Alert
        checkSpeedAlert(speedKmh)
    }

    private fun checkSpeedAlert(speedKmh: Double) {
        val currentTime = System.currentTimeMillis()
        val threshold = _speedThreshold.value ?: 80.0
        if (speedKmh >= threshold && (currentTime - lastAlertTime) > alertCooldownMs) {
            triggerAlert()
            lastAlertTime = currentTime
        }
    }

    private fun triggerAlert() {
        _showAlert.postValue(true)
        _safetyMessage.postValue(getRandomSafetyMessage())
        vibrateDevice()
    }

    private fun vibrateDevice() {
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun getRandomSafetyMessage(): String {
        return """
            ⚠️ Slow down!

            You're driving too fast. Please keep your speed under control.

            Remember, someone at home is waiting for you. Your safety matters more than anything. ❤️
        """.trimIndent()
    }

    fun dismissAlert() {
        _showAlert.value = false
    }
}
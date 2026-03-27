package com.example.speedtracker.logic

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.example.speedtracker.R
import com.example.speedtracker.ui.MainActivity
import com.example.speedtracker.utils.Constants
import com.google.android.gms.location.*

class LocationService : android.app.Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val currentSpeed = MutableLiveData<Float>()
        val pathPoints = MutableLiveData<MutableList<Location>>()
        val distanceTraveled = MutableLiveData<Float>()
        
        private var lastLocation: Location? = null
        private var totalDistance = 0f

        fun resetValues() {
            isTracking.postValue(false)
            currentSpeed.postValue(0f)
            pathPoints.postValue(mutableListOf())
            distanceTraveled.postValue(0f)
            lastLocation = null
            totalDistance = 0f
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        resetValues()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    startForegroundService()
                    startTracking()
                }
                Constants.ACTION_STOP_SERVICE -> {
                    stopTracking()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        isTracking.postValue(true)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, Constants.LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(Constants.FASTEST_LOCATION_INTERVAL)
            .build()

        fusedLocationProviderClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopTracking() {
        isTracking.postValue(false)
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value == true) {
                for (location in result.locations) {
                    updateLocationData(location)
                }
            }
        }
    }

    private fun updateLocationData(location: Location) {
        currentSpeed.postValue(location.speed)
        
        val points = pathPoints.value ?: mutableListOf()
        points.add(location)
        pathPoints.postValue(points)

        lastLocation?.let {
            val distance = it.distanceTo(location)
            totalDistance += distance
            distanceTraveled.postValue(totalDistance)
        }
        lastLocation = location
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Speed Tracker")
            .setContentText("Tracking your speed...")
            .setContentIntent(getMainActivityPendingIntent())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Constants.NOTIFICATION_ID,
                notificationBuilder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(Constants.NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

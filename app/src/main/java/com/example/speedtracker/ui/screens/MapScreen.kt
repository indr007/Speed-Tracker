package com.example.speedtracker.ui.screens

import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.speedtracker.data.model.Trip
import com.example.speedtracker.ui.HistoryViewModel
import com.example.speedtracker.ui.MainViewModel
import com.example.speedtracker.ui.MainActivity
import com.example.speedtracker.ui.navigation.Screen
import com.example.speedtracker.ui.subscription.LockedMapScreen
import com.example.speedtracker.ui.subscription.SubscriptionViewModel
import androidx.compose.runtime.collectAsState
import com.example.speedtracker.ui.theme.GrayStats
import com.example.speedtracker.ui.theme.PrimaryOrange
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.example.speedtracker.utils.RewardedAdManager
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val factory = activity.factory
    
    val viewModel: MainViewModel = viewModel(factory = factory)
    val subViewModel: SubscriptionViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    
    val tripId = navController.currentBackStackEntry?.arguments?.getLong("tripId") ?: -1L
    
    val isSubscribed by subViewModel.isSubscribed.collectAsState()
    val hasTemporaryAccess by subViewModel.hasTemporaryAccess.collectAsState()
    
    // Rewarded Ad Manager
    val rewardedAdManager = remember { RewardedAdManager(context) }
    
    // Load Rewarded Ad
    LaunchedEffect(isSubscribed) {
        if (!isSubscribed) {
            rewardedAdManager.loadAd()
        }
    }

    // Access Control
    if (!isSubscribed && !hasTemporaryAccess) {
        LockedMapScreen(
            onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
            onWatchAd = {
                rewardedAdManager.showAd(activity) {
                    subViewModel.grantTemporaryAccess()
                    Toast.makeText(context, "Map unlocked for this session", Toast.LENGTH_LONG).show()
                }
            },
            isLoadingAd = rewardedAdManager.isLoading.value,
            isAdReady = rewardedAdManager.isAdReady.value
        )
        return
    }

    // Initialize OSM
    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = "SpeedTrackerApp/1.0 (" + context.packageName + ")"
        true
    }

    // Data for Map
    val currentPathPoints by com.example.speedtracker.logic.LocationService.pathPoints.observeAsState(mutableListOf())
    var historicalTrip by remember { mutableStateOf<Trip?>(null) }
    
    LaunchedEffect(tripId) {
        if (tripId != -1L) {
            historicalTrip = historyViewModel.getTripById(tripId)
        }
    }

    val geoPoints = remember(currentPathPoints, historicalTrip) {
        if (tripId != -1L && historicalTrip != null) {
            parsePathPoints(historicalTrip!!.tourPath)
        } else {
            currentPathPoints.map { GeoPoint(it.latitude, it.longitude) }
        }
    }

    val mapView = remember { MapView(context) }
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            if (tripId == -1L) {
                enableMyLocation()
                enableFollowLocation()
            }
        }
    }

    // Lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    if (tripId == -1L) myLocationOverlay.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    myLocationOverlay.disableMyLocation()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Map Updates
    LaunchedEffect(geoPoints) {
        if (geoPoints.isNotEmpty()) {
            if (tripId == -1L) {
                mapView.controller.animateTo(geoPoints.last())
            } else {
                mapView.controller.setCenter(geoPoints.first())
                mapView.controller.setZoom(15.0)
            }
            
            mapView.overlays.removeAll { it is Polyline }
            val polyline = Polyline().apply {
                setPoints(geoPoints)
                outlinePaint.color = android.graphics.Color.parseColor("#FF9800")
                outlinePaint.strokeWidth = 12f
            }
            mapView.overlays.add(polyline)
            mapView.invalidate()
        }
    }

    LaunchedEffect(Unit) {
        if (!mapView.overlays.contains(myLocationOverlay) && tripId == -1L) {
            mapView.overlays.add(myLocationOverlay)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tripId != -1L) "Trip Replay" else "Live Tracking") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(17.0)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (tripId == -1L) {
                LiveStatsCard(viewModel)
            } else {
                historicalTrip?.let { ReplayStatsCard(it) }
            }
        }
    }
}

@Composable
fun BoxScope.LiveStatsCard(viewModel: MainViewModel) {
    val currentSpeed by viewModel.currentSpeedKmh.observeAsState(0.0)
    val distance by viewModel.distanceKm.observeAsState(0.0)
    val avgSpeed by viewModel.avgSpeedKmh.observeAsState(0.0)

    Card(
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = String.format("%.1f", currentSpeed), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = PrimaryOrange)
                Text(text = "km/h", style = MaterialTheme.typography.labelMedium, color = GrayStats)
            }
            VerticalDivider(modifier = Modifier.height(60.dp).padding(horizontal = 16.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                MapStatItem("DISTANCE", String.format("%.2f km", distance))
                MapStatItem("AVG SPEED", String.format("%.1f", avgSpeed))
            }
        }
    }
}

@Composable
fun BoxScope.ReplayStatsCard(trip: Trip) {
    Card(
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            MapStatItem("TOTAL DIST", String.format("%.2f km", trip.distance))
            MapStatItem("MAX SPEED", String.format("%.1f", trip.maxSpeed))
            MapStatItem("AVG SPEED", String.format("%.1f", trip.avgSpeed))
        }
    }
}

@Composable
fun MapStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GrayStats)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

fun parsePathPoints(json: String): List<GeoPoint> {
    val points = mutableListOf<GeoPoint>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            points.add(GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude")))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return points
}

package com.example.speedtracker.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavHostController
import com.example.speedtracker.data.model.Trip
import com.example.speedtracker.ui.HistoryViewModel
import com.example.speedtracker.ui.MainActivity
import com.example.speedtracker.ui.navigation.Screen
import com.example.speedtracker.ui.subscription.SubscriptionViewModel
import com.example.speedtracker.ui.subscription.LockedMapScreen
import com.example.speedtracker.ui.theme.GrayStats
import com.example.speedtracker.ui.theme.PrimaryOrange
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavHostController) {
    val context = LocalContext.current as MainActivity
    val viewModel: HistoryViewModel = viewModel(factory = context.factory)
    val subViewModel: SubscriptionViewModel = viewModel(factory = context.factory)
    
    val isSubscribed by subViewModel.isSubscribed.collectAsState()
    val trips by viewModel.allTrips.observeAsState(emptyList())

    if (!isSubscribed) {
        LockedMapScreen(
            onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
            onWatchAd = { /* Optional: Navigate to map directly or show ad */ }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSubscribed && trips.isNotEmpty()) {
                        IconButton(onClick = { exportTripsToCsv(context, trips) }) {
                            Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = PrimaryOrange)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (isSubscribed) {
                    SmartInsightsCard(trips)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                HistoryAnalytics(trips)
            }
            items(trips) { trip ->
                TripCard(trip, onReplay = {
                    navController.navigate(Screen.Maps.createRoute(trip.id))
                })
            }
        }
    }
}

@Composable
fun TripCard(trip: Trip, onReplay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onReplay() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                Text(
                    text = sdf.format(Date(trip.startTime)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = Icons.Default.Map, 
                    contentDescription = "Replay", 
                    tint = PrimaryOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatSubItem("DIST", String.format("%.2f km", trip.distance))
                StatSubItem("MAX", String.format("%.1f", trip.maxSpeed))
                StatSubItem("AVG", String.format("%.1f", trip.avgSpeed))
            }
        }
    }
}

@Composable
fun HistoryAnalytics(trips: List<Trip>) {
    if (trips.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Speed Analytics (Last ${trips.size.coerceAtMost(7)} trips)",
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryOrange,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SpeedLineChart(trips.take(7).reversed())
        }
    }
}

@Composable
fun SpeedLineChart(trips: List<Trip>) {
    val maxSpeed = trips.map { it.maxSpeed }.maxOrNull()?.toFloat() ?: 100f
    
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height
        val spacing = width / (trips.size + 1)
        
        // Draw Y-axis guide lines
        for (i in 0..4) {
            val y = height - (i * height / 4)
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        val points = trips.mapIndexed { index, trip ->
            androidx.compose.ui.geometry.Offset(
                x = spacing * (index + 1),
                y = height - (trip.maxSpeed.toFloat() / maxSpeed * height)
            )
        }

        // Draw Path
        if (points.isNotEmpty()) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(path = path, color = PrimaryOrange, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
            
            // Draw Points
            points.forEach { point ->
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = point)
                drawCircle(color = PrimaryOrange, radius = 2.dp.toPx(), center = point)
            }
        }
    }
}

@Composable
fun StatSubItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GrayStats)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SmartInsightsCard(trips: List<Trip>) {
    if (trips.size < 2) return
    
    val latest = trips[0]
    val previous = trips[1]
    
    val distanceDiff = latest.distance - previous.distance
    val isMoreDistance = distanceDiff > 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryOrange)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Smart Insight",
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryOrange,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isMoreDistance) {
                        "You traveled ${String.format("%.1f", distanceDiff)} km more than your previous trip! Keep it up! 🚀"
                    } else {
                        "You've been 10% safer this week by maintaining a steady speed. Great job! 🛡️"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}

fun exportTripsToCsv(context: Context, trips: List<Trip>) {
    val csvHeader = "ID,StartTime,EndTime,MaxSpeed,AvgSpeed,Distance\n"
    val csvBody = trips.joinToString("\n") { trip ->
        "${trip.id},${trip.startTime},${trip.endTime},${trip.maxSpeed},${trip.avgSpeed},${trip.distance}"
    }
    val csvData = csvHeader + csvBody
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Speed Tracker - Trip History Export")
        putExtra(Intent.EXTRA_TEXT, csvData)
    }
    context.startActivity(Intent.createChooser(intent, "Export Trip Data"))
}

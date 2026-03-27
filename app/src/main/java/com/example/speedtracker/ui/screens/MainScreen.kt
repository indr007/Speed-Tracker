package com.example.speedtracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.speedtracker.ui.MainActivity
import com.example.speedtracker.ui.MainViewModel
import com.example.speedtracker.ui.navigation.Screen
import com.example.speedtracker.ui.theme.GrayStats
import com.example.speedtracker.ui.theme.PrimaryOrange
import com.example.speedtracker.ui.theme.SurfaceGray
import com.example.speedtracker.ui.components.AdBanner
import com.example.speedtracker.ui.components.Speedometer
import com.example.speedtracker.ui.subscription.SubscriptionViewModel

@Composable
fun MainScreen(viewModel: MainViewModel, navController: NavHostController) {
    val context = LocalContext.current as MainActivity
    val subViewModel: SubscriptionViewModel = viewModel(factory = context.factory)
    
    val isPremium by subViewModel.isSubscribed.collectAsState()
    val currentSpeed by viewModel.currentSpeedKmh.observeAsState(0.0)
    val maxSpeed by viewModel.maxSpeedKmh.observeAsState(0.0)
    val avgSpeed by viewModel.avgSpeedKmh.observeAsState(0.0)
    val distance by viewModel.distanceKm.observeAsState(0.0)
    val isTracking by viewModel.isTracking.observeAsState(false)
    
    val showAlert by viewModel.showAlert.observeAsState(false)
    val safetyMessage by viewModel.safetyMessage.observeAsState("")

    // Speed alert dialog
    if (showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { 
                Text("SAFETY ALERT", color = Color.Red, fontWeight = FontWeight.Black)
            },
            text = { 
                Text(
                    text = safetyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) {
                    Text("OK, SLOWING DOWN", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF2C2C2C),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A1A), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. AdBanner at the TOP (Only for non-premium)
            AdBanner(isPremium = isPremium, modifier = Modifier.padding(bottom = 8.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { navController.navigate(Screen.Maps.route) },
                        modifier = Modifier.background(SurfaceGray, CircleShape)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Map", tint = Color.White)
                    }
                    
                    Text(
                        text = "SPEED TRACKER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                if (!isPremium) navController.navigate(Screen.Paywall.route)
                                else navController.navigate(Screen.History.route) 
                            },
                            modifier = Modifier.background(
                                if (!isPremium) PrimaryOrange.copy(alpha = 0.2f) else SurfaceGray, 
                                CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = if (!isPremium) Icons.Default.RocketLaunch else Icons.Default.History, 
                                contentDescription = if (!isPremium) "Premium" else "History", 
                                tint = if (!isPremium) PrimaryOrange else Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { navController.navigate(Screen.Settings.route) },
                            modifier = Modifier.background(SurfaceGray, CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Speedometer Dashboard
                val theme by viewModel.speedometerTheme.observeAsState("Standard")
                Speedometer(speed = currentSpeed, theme = theme)

                Spacer(modifier = Modifier.height(60.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernStatCard("MAX", String.format("%.1f", maxSpeed), Icons.Default.VerticalAlignTop, Modifier.weight(1f))
                    ModernStatCard("AVG", String.format("%.1f", avgSpeed), Icons.Default.Speed, Modifier.weight(1f))
                    ModernStatCard("DIST", String.format("%.2f", distance), Icons.Default.Route, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(40.dp))

                // Control Button
                Button(
                    onClick = { 
                        if (isTracking) viewModel.stopTrip() else viewModel.startTrip()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTracking) Color(0xFFE53935) else PrimaryOrange
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isTracking) "STOP SESSION" else "START TRACKING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ModernStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryOrange.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

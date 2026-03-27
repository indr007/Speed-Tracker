package com.example.speedtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.speedtracker.ui.MainActivity
import com.example.speedtracker.ui.MainViewModel
import com.example.speedtracker.ui.navigation.Screen
import com.example.speedtracker.ui.subscription.SubscriptionViewModel
import com.example.speedtracker.ui.theme.PrimaryOrange
import com.example.speedtracker.ui.theme.SurfaceGray
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current as MainActivity
    val subViewModel: SubscriptionViewModel = viewModel(factory = context.factory)
    
    val isPremium by subViewModel.isSubscribed.collectAsState()
    val speedThreshold by viewModel.speedThreshold.observeAsState(80.0)
    val currentTheme by viewModel.speedometerTheme.observeAsState("Standard")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Premium Status Card
            PremiumStatusCard(isPremium) {
                navController.navigate(Screen.Paywall.route)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Speed Alert Section
            SettingsSectionTitle("Speed Alert")
            SpeedLimitControl(
                isPremium = isPremium,
                currentLimit = speedThreshold,
                onLimitChange = { viewModel.setSpeedThreshold(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Speedometer Theme Section
            SettingsSectionTitle("Speedometer Theme")
            ThemeSelector(
                isPremium = isPremium,
                currentTheme = currentTheme,
                onThemeSelect = { viewModel.setSpeedometerTheme(it) }
            )
        }
    }
}

@Composable
fun PremiumStatusCard(isPremium: Boolean, onUpgrade: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPremium) { onUpgrade() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) Color(0xFF1B5E20) else SurfaceGray
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPremium) Icons.Default.Info else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isPremium) Color.White else PrimaryOrange
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isPremium) "Premium Member" else "Free Version",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isPremium) "Thank you for your support!" else "Tap to unlock premium features",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = PrimaryOrange,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SpeedLimitControl(
    isPremium: Boolean,
    currentLimit: Double,
    onLimitChange: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alert Threshold: ${currentLimit.toInt()} km/h",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                if (!isPremium) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
            
            Slider(
                value = currentLimit.toFloat(),
                onValueChange = { if (isPremium) onLimitChange(it.toDouble()) },
                valueRange = 20f..180f,
                steps = 16,
                enabled = isPremium,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryOrange,
                    activeTrackColor = PrimaryOrange,
                    inactiveTrackColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun ThemeSelector(
    isPremium: Boolean,
    currentTheme: String,
    onThemeSelect: (String) -> Unit
) {
    val themes = listOf("Standard", "Sport", "Future")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themes.forEach { theme ->
            ThemeOption(
                name = theme,
                isSelected = currentTheme == theme,
                isPremium = isPremium,
                onClick = { if (isPremium || theme == "Standard") onThemeSelect(theme) }
            )
        }
    }
}

@Composable
fun RowScope.ThemeOption(
    name: String,
    isSelected: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    val isLocked = !isPremium && name != "Standard"
    
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable(enabled = !isLocked) { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryOrange) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryOrange.copy(alpha = 0.1f) else SurfaceGray
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLocked) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = if (isSelected) PrimaryOrange else Color.Gray)
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) PrimaryOrange else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

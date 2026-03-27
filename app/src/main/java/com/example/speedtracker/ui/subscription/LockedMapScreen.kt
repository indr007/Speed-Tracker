package com.example.speedtracker.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.speedtracker.ui.theme.PrimaryOrange

@Composable
fun LockedMapScreen(
    onNavigateToPaywall: () -> Unit,
    onWatchAd: () -> Unit,
    isLoadingAd: Boolean = false,
    isAdReady: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(100.dp),
                tint = PrimaryOrange
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Unlock Map Tracking",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Get access to live GPS tracking and route drawings by joining premium or watching a quick ad.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onNavigateToPaywall,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Go Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onWatchAd,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isAdReady && !isLoadingAd,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.Gray
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isAdReady) Color.White else Color.Gray
                )
            ) {
                if (isLoadingAd) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Loading Ad...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(
                        if (isAdReady) "Watch Ad to Unlock (Free)" else "Ad Not Ready",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

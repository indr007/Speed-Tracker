package com.example.speedtracker.ui.subscription

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedtracker.ui.theme.PrimaryOrange

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.widget.Toast

@Composable
fun PaywallScreen(viewModel: SubscriptionViewModel, onBack: () -> Unit) {
    val context = LocalContext.current as Activity
    val paymentStatus by viewModel.paymentStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Show toast for payment status updates
    LaunchedEffect(paymentStatus) {
        paymentStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1A1A), Color.Black)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Icon(
            imageVector = Icons.Default.RocketLaunch,
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Go Premium 🚀",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Unlock the full potential of Speed Tracker",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Benefits List
        val benefits = listOf("Live Map Tracking", "Route History", "Advanced Stats")
        benefits.forEach { benefit ->
            BenefitItem(benefit)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.buySubscription(context) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Subscribe ₹49/month",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        TextButton(onClick = { viewModel.restorePurchases() }) {
            Text(text = "Restore Purchase", color = Color.Gray)
        }

        TextButton(onClick = onBack) {
            Text(text = "Not Now", color = Color.DarkGray)
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

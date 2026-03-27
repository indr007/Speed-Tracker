package com.example.speedtracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(isPremium: Boolean, modifier: Modifier = Modifier) {
    if (isPremium) return
    
    // Use test banner ID: ca-app-pub-3940256099942544/6300978111
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-5189732760491544/3253082046"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

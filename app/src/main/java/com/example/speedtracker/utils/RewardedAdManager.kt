package com.example.speedtracker.utils

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback

class RewardedAdManager(private val context: Context) {

    // Real AdMob Rewarded Ad Unit ID
    private val REWARDED_AD_UNIT_ID = "ca-app-pub-5189732760491544/2965475301"

    private var _rewardedAd = mutableStateOf<RewardedAd?>(null)
    val rewardedAd: State<RewardedAd?> = _rewardedAd

    private var _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private var _isAdReady = mutableStateOf(false)
    val isAdReady: State<Boolean> = _isAdReady

    fun loadAd() {
        if (_isLoading.value || _rewardedAd.value != null) return

        _isLoading.value = true
        _isAdReady.value = false
        
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    _rewardedAd.value = null
                    _isLoading.value = false
                    _isAdReady.value = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    _rewardedAd.value = ad
                    _isLoading.value = false
                    _isAdReady.value = true
                    
                    setupAdCallbacks(ad)
                }
            }
        )
    }

    private fun setupAdCallbacks(ad: RewardedAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                _rewardedAd.value = null
                _isAdReady.value = false
                // Reload ad for next time
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                _rewardedAd.value = null
                _isAdReady.value = false
                loadAd()
            }
        }
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit) {
        _rewardedAd.value?.let { ad ->
            ad.show(activity) { _ ->
                onRewardEarned()
            }
        } ?: run {
            // Ad not ready, reload
            loadAd()
        }
    }
}

package com.example.speedtracker.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.speedtracker.data.repository.TripRepository
import com.example.speedtracker.data.subscription.BillingRepository
import com.example.speedtracker.data.subscription.SubscriptionStatusManager
import com.example.speedtracker.ui.subscription.SubscriptionViewModel
import com.example.speedtracker.data.payment.RazorpayRepository

class ViewModelFactory(
    private val repository: TripRepository,
    private val application: Application? = null,
    private val billingRepository: BillingRepository? = null,
    private val statusManager: SubscriptionStatusManager? = null,
    private val razorpayRepository: RazorpayRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application!!, repository, statusManager!!) as T
        }
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(billingRepository!!, statusManager!!, razorpayRepository!!) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.speedtracker.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedtracker.data.subscription.BillingRepository
import com.example.speedtracker.data.subscription.SubscriptionStatusManager
import com.example.speedtracker.data.payment.RazorpayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val billingRepository: BillingRepository,
    private val statusManager: SubscriptionStatusManager,
    private val razorpayRepository: RazorpayRepository
) : ViewModel() {

    val isSubscribed: StateFlow<Boolean> = statusManager.isSubscribed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _paymentStatus = MutableStateFlow<String?>(null)
    val paymentStatus = _paymentStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _hasTemporaryAccess = MutableStateFlow(false)
    val hasTemporaryAccess = _hasTemporaryAccess.asStateFlow()

    val productDetails = billingRepository.productDetails
    val isBillingConnected = billingRepository.isBillingConnected

    init {
        viewModelScope.launch {
            billingRepository.billingMessage.collect { message ->
                _paymentStatus.value = message
                _isLoading.value = false
            }
        }
    }

    fun buySubscription(activity: Activity) {
        _isLoading.value = true
        billingRepository.launchPurchaseFlow(activity)
    }

    fun buyWithRazorpay(activity: Activity, amount: Int) {
        viewModelScope.launch {
            razorpayRepository.startPayment(activity, amount)
        }
    }

    fun verifyRazorpayPayment(orderId: String, paymentId: String, signature: String) {
        viewModelScope.launch {
            val success = razorpayRepository.verifyPayment(orderId, paymentId, signature)
            if (success) {
                statusManager.setSubscribed(true)
                _paymentStatus.value = "Payment Successful!"
            } else {
                _paymentStatus.value = "Payment Verification Failed"
            }
        }
    }

    fun restorePurchases() {
        billingRepository.checkActiveSubscriptions()
    }

    fun grantTemporaryAccess() {
        _hasTemporaryAccess.value = true
    }
}

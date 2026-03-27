package com.example.speedtracker.data.subscription

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingRepository(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val statusManager: SubscriptionStatusManager
) : PurchasesUpdatedListener {

    private val tag = "BillingRepository"

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _isBillingConnected = MutableStateFlow(false)
    val isBillingConnected = _isBillingConnected.asStateFlow()

    private val _billingMessage = MutableSharedFlow<String>(replay = 0)
    val billingMessage = _billingMessage.asSharedFlow()

    init {
        startConnection()
    }

    fun startConnection() {
        Log.d(tag, "Starting connection...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(tag, "Billing setup finished: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingConnected.value = true
                    querySubscriptionDetails()
                    checkActiveSubscriptions()
                } else {
                    externalScope.launch {
                        _billingMessage.emit("Billing Error: ${billingResult.debugMessage}")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(tag, "Billing service disconnected")
                _isBillingConnected.value = false
                // Retry connection
                externalScope.launch {
                    delay(3000)
                    startConnection()
                }
            }
        })
    }

    private fun querySubscriptionDetails() {
        Log.d(tag, "Querying subscription details for: premium_map")
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("premium_map")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult: BillingResult, productDetailsList: List<ProductDetails> ->
            Log.d(tag, "Query response: ${billingResult.responseCode} list size: ${productDetailsList.size}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isEmpty()) {
                    Log.e(tag, "Product list is empty! Ensure ID 'premium_map' matches Play Console.")
                    externalScope.launch {
                        _billingMessage.emit("Product 'premium_map' not found in Play Store.")
                    }
                }
                _productDetails.value = productDetailsList.firstOrNull()
            } else {
                externalScope.launch {
                    _billingMessage.emit("Failed to fetch product: ${billingResult.debugMessage}")
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        Log.d(tag, "launchPurchaseFlow called")
        if (!billingClient.isReady) {
            Log.e(tag, "BillingClient is not ready")
            externalScope.launch {
                _billingMessage.emit("Billing system is not ready. Reconnecting...")
            }
            startConnection()
            return
        }

        val productDetails = _productDetails.value
        if (productDetails == null) {
            Log.e(tag, "Product details null in launchPurchaseFlow")
            externalScope.launch {
                _billingMessage.emit("Product details not loaded. Please try again.")
            }
            querySubscriptionDetails()
            return
        }
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            externalScope.launch {
                _billingMessage.emit("Subscription offer not found.")
            }
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        Log.d(tag, "launchBillingFlow response: ${billingResult.responseCode} - ${billingResult.debugMessage}")
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            externalScope.launch {
                _billingMessage.emit("Failed to launch billing: ${billingResult.debugMessage}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                externalScope.launch {
                    _billingMessage.emit("Purchase cancelled.")
                }
            }
            else -> {
                externalScope.launch {
                    _billingMessage.emit("Billing error: ${billingResult.debugMessage}")
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult: BillingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        externalScope.launch {
                            statusManager.setSubscribed(true)
                            _billingMessage.emit("Subscription activated!")
                        }
                    } else {
                        externalScope.launch {
                            _billingMessage.emit("Failed to acknowledge purchase: ${billingResult.debugMessage}")
                        }
                    }
                }
            } else {
                externalScope.launch {
                    statusManager.setSubscribed(true)
                    _billingMessage.emit("Subscription restored!")
                }
            }
        }
    }

    fun checkActiveSubscriptions() {
        Log.d(tag, "checkActiveSubscriptions called")
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult: BillingResult, purchases: List<Purchase> ->
            Log.d(tag, "checkActiveSubscriptions response: ${billingResult.responseCode} count: ${purchases.size}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                externalScope.launch {
                    statusManager.setSubscribed(hasPremium)
                    if (hasPremium) {
                        _billingMessage.emit("Premium access restored!")
                    } else {
                        // _billingMessage.emit("No active subscription found.") // Optional
                    }
                }
            } else {
                externalScope.launch {
                    _billingMessage.emit("Failed to restore: ${billingResult.debugMessage}")
                }
            }
        }
    }
}

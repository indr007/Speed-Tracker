package com.example.speedtracker.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.speedtracker.data.local.AppDatabase
import com.example.speedtracker.data.repository.TripRepository
import com.example.speedtracker.ui.navigation.Screen
import com.example.speedtracker.ui.screens.MainScreen
import com.example.speedtracker.ui.screens.HistoryScreen
import com.example.speedtracker.ui.screens.MapScreen
import com.example.speedtracker.ui.screens.SettingsScreen
import com.example.speedtracker.ui.theme.SpeedTrackerTheme
import com.example.speedtracker.data.subscription.BillingRepository
import com.example.speedtracker.data.subscription.SubscriptionStatusManager
import com.example.speedtracker.ui.subscription.SubscriptionViewModel
import com.example.speedtracker.ui.subscription.PaywallScreen
import com.example.speedtracker.data.payment.RazorpayRepository
import com.google.android.gms.ads.MobileAds
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private lateinit var subViewModel: SubscriptionViewModel
    lateinit var factory: ViewModelFactory

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (!result.values.all { it }) {
            Toast.makeText(this, "Permissions required for tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()
        MobileAds.initialize(this)

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TripRepository(database.tripDao)
        val statusManager = SubscriptionStatusManager(applicationContext)
        val billingRepository = BillingRepository(applicationContext, lifecycleScope, statusManager)
        val razorpayRepository = RazorpayRepository(applicationContext)
        
        factory = ViewModelFactory(
            repository = repository, 
            application = application,
            billingRepository = billingRepository,
            statusManager = statusManager,
            razorpayRepository = razorpayRepository
        )

        setContent {
            SpeedTrackerTheme {
                val navController = rememberNavController()
                subViewModel = viewModel(factory = factory)

                NavHost(
                    navController = navController,
                    startDestination = Screen.Speedometer.route
                ) {
                    composable(Screen.Speedometer.route) {
                        val viewModel: MainViewModel = viewModel(factory = factory)
                        MainScreen(viewModel, navController)
                    }

                    composable(Screen.History.route) {
                        HistoryScreen(navController)
                    }

                    composable(
                        route = Screen.Maps.route,
                        arguments = listOf(
                            navArgument("tripId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            }
                        )
                    ) {
                        MapScreen(navController)
                    }

                    composable(Screen.Paywall.route) {
                        PaywallScreen(
                            subViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) {
                        val viewModel: MainViewModel = viewModel(factory = factory)
                        SettingsScreen(viewModel, navController)
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId ?: ""
        val signature = paymentData?.signature ?: ""
        if (razorpayPaymentId != null && orderId.isNotEmpty() && signature.isNotEmpty()) {
            subViewModel.verifyRazorpayPayment(orderId, razorpayPaymentId, signature)
        }
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        Toast.makeText(this, "Payment Failed: $description", Toast.LENGTH_LONG).show()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}
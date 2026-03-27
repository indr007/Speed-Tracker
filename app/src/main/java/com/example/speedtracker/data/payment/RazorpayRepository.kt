package com.example.speedtracker.data.payment

import android.app.Activity
import android.util.Log
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import android.content.Context

class RazorpayRepository(private val context: Context) {

    private val api: RazorpayService

    init {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") 
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        api = retrofit.create(RazorpayService::class.java)
        Checkout.preload(context.applicationContext)
    }

    suspend fun startPayment(activity: Activity, amount: Int) {
        try {
            val orderResponse = api.createOrder(OrderRequest(amount))
            val checkout = Checkout()
            // In a real app, this should be fetched from BuildConfig or Backend
            checkout.setKeyID("rzp_test_YOUR_KEY_ID") 

            val options = JSONObject()
            options.put("name", "Speed Tracker Premium")
            options.put("description", "Monthly Subscription")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("order_id", orderResponse.order_id)
            options.put("theme.color", "#FF9800")
            options.put("currency", orderResponse.currency)
            options.put("amount", orderResponse.amount)
            
            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            checkout.open(activity, options)

        } catch (e: Exception) {
            Log.e("Razorpay", "Error in starting payment: ${e.message}")
        }
    }

    suspend fun verifyPayment(orderId: String, paymentId: String, signature: String): Boolean {
        return try {
            val response = api.verifyPayment(VerificationRequest(orderId, paymentId, signature))
            response.status == "success"
        } catch (e: Exception) {
            Log.e("Razorpay", "Verification failed: ${e.message}")
            false
        }
    }
}

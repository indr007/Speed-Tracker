package com.example.speedtracker.data.payment

import retrofit2.http.Body
import retrofit2.http.POST

data class OrderRequest(val amount: Int, val currency: String = "INR")
data class OrderResponse(val order_id: String, val amount: Int, val currency: String)

data class VerificationRequest(
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String
)
data class VerificationResponse(val status: String, val message: String)

interface RazorpayService {
    @POST("create-order")
    suspend fun createOrder(@Body request: OrderRequest): OrderResponse

    @POST("verify-payment")
    suspend fun verifyPayment(@Body request: VerificationRequest): VerificationResponse
}

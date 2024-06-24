package com.example.optimate.loginAndRegister
import com.google.gson.annotations.SerializedName

data class SendMessageDTO(
    val deviceToken: String?, // Renamed from 'to' to match server-side expectation
    val title: String?,
    val body: String?
)




data class PaymentData(
    @SerializedName("token") val token: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("currency") val currency: String
)
package com.example.trainingappmobile

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("api_key") val apiKey: String? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
) {
    fun hasError(): Boolean = !error.isNullOrEmpty()
    fun isSuccessful(): Boolean = !apiKey.isNullOrEmpty()
}
package com.example.trainingappmobile

data class LoginResponse(
    val api_key: String?,
    val device_id: String?,
    val user_role: String?,
    val error: String?
)
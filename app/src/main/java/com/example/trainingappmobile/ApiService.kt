package com.example.trainingappmobile

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("api/v1/sessions")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/v1/planilha")
    fun getPlanilha(
        @Header("Authorization") authHeader: String,
        @Header("Device-ID") deviceId: String
    ): Call<PlanilhaResponse>

    @DELETE("api/v1/sessions")
    fun logout(
        @Header("Authorization") authHeader: String,
        @Header("Device-ID") deviceId: String
    ): Call<Void>
}
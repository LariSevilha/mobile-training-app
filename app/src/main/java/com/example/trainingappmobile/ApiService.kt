package com.example.trainingappmobile

import retrofit2.Call
import retrofit2.http.*
import okhttp3.ResponseBody

interface ApiService {

    @FormUrlEncoded
    @POST("auth/login")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("device_id") deviceId: String
    ): Call<LoginResponse>

    @DELETE("auth/logout")
    fun logout(
        @Header("Authorization") authorization: String,
        @Query("device_id") deviceId: String
    ): Call<ResponseBody>

    @GET("planilhas")
    fun getPlanilha(
        @Header("Authorization") authorization: String,
        @Query("device_id") deviceId: String
    ): Call<PlanilhaResponse>
}

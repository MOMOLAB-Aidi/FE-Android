package com.example.momolabfe.remote.fcm.service

import com.example.momolabfe.BuildConfig
import com.example.momolabfe.remote.fcm.model.FcmTokenRequest
import com.example.momolabfe.utils.ApiResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST

interface FcmService {
    @POST("/api/v1/fcm/token")
    suspend fun registerToken(@Body request: FcmTokenRequest): Response<Unit>

    @PATCH("/api/v1/fcm/token")
    suspend fun deactivateToken(@Body request: FcmTokenRequest): Response<Unit>

    companion object {
        val instance: FcmService by lazy {
            Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL_SPRING)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FcmService::class.java)
        }
    }
}
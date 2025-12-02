package com.example.momolabfe.remote.fcm.service

import com.example.momolabfe.remote.fcm.model.FcmTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST

interface FcmService {
    @POST("/api/v1/fcm/token")
    suspend fun registerToken(@Body request: FcmTokenRequest): Response<Unit>

    @PATCH("/api/v1/fcm/token")
    suspend fun deactivateToken(@Body request: FcmTokenRequest): Response<Unit>
}
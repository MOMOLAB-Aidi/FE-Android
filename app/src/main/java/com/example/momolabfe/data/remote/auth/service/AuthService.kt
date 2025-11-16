package com.example.momolabfe.data.remote.auth.service

import com.example.momolabfe.data.remote.auth.data.AuthRequest
import com.example.momolabfe.data.remote.auth.data.AuthResponse
import com.example.momolabfe.utils.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/api/v1/login")
    suspend fun login(@Body request: AuthRequest): Response<ApiResponse<AuthResponse>>
}
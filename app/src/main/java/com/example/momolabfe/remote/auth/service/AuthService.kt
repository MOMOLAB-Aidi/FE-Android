package com.example.momolabfe.remote.auth.service

import com.example.momolabfe.remote.auth.data.LoginRequest
import com.example.momolabfe.remote.auth.data.LoginResponse
import com.example.momolabfe.remote.auth.data.TokenResponse
import com.example.momolabfe.remote.auth.data.TokenRequest
import com.example.momolabfe.utils.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("/api/v1/auth/reissue")
    suspend fun reissue(@Body request: TokenRequest): Response<ApiResponse<TokenResponse>>

    @POST("/api/v1/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
}
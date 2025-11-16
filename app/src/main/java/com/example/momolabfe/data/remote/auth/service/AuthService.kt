package com.example.momolabfe.data.remote.login.service

import com.example.momolabfe.data.remote.login.data.LoginRequest
import com.example.momolabfe.data.remote.login.data.LoginResponse
import com.example.momolabfe.utils.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginService {

    @POST("/api/v1/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>
}
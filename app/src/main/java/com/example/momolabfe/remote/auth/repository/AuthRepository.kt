package com.example.momolabfe.remote.auth.repository

import android.util.Log
import com.example.momolabfe.remote.auth.data.LoginRequest
import com.example.momolabfe.remote.auth.data.LoginResponse
import com.example.momolabfe.remote.auth.service.AuthService
import com.example.momolabfe.utils.NoAuthRetrofit
import com.example.momolabfe.utils.handleApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor (
    @NoAuthRetrofit private val authService: AuthService,
){

    suspend fun login(request: LoginRequest): Result<LoginResponse> = runCatching {
        val response = authService.login(request)
        Log.d("Login", "Login API 호출 코드: ${response.code()}")
        handleApiResponse(response)
    }
}
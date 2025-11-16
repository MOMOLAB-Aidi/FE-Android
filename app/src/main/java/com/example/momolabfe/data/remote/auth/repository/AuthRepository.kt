package com.example.momolabfe.data.remote.auth.repository

import android.util.Log
import com.example.momolabfe.data.remote.auth.data.AuthRequest
import com.example.momolabfe.data.remote.auth.data.AuthResponse
import com.example.momolabfe.data.remote.auth.service.AuthService
import com.example.momolabfe.utils.handleApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor (
    private val authService: AuthService
){

    suspend fun login(request: AuthRequest): Result<AuthResponse> = runCatching {
        val response = authService.login(request)
        Log.d("Login", "response = ${response.body()}")
        handleApiResponse(response)
    }
}
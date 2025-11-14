package com.example.momolabfe.data.remote.login.repository

import android.util.Log
import com.example.momolabfe.data.remote.login.data.LoginRequest
import com.example.momolabfe.data.remote.login.data.LoginResponse
import com.example.momolabfe.data.remote.login.service.LoginService
import com.example.momolabfe.utils.handleApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor (
    private val authService: LoginService
){

    suspend fun login(request: LoginRequest): Result<LoginResponse> = runCatching {
        val response = authService.login(request)
        Log.d("Login", "response = ${response.body()}")
        handleApiResponse(response)
    }
}
package com.example.momolabfe.remote.user.service

import com.example.momolabfe.remote.user.model.MyPageResponse
import com.example.momolabfe.remote.user.model.UpdatePassword
import com.example.momolabfe.utils.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserService {

    @GET("/api/v1/users/mypage")
    suspend fun getMyPage(): Response<ApiResponse<MyPageResponse>>

    @PATCH("/api/v1/users/password")
    suspend fun updatePassword(@Body request: UpdatePassword): Response<ApiResponse<Unit>>
}
package com.example.momolabfe.remote.user.service

import com.example.momolabfe.remote.user.model.MyPageResponse
import com.example.momolabfe.utils.ApiResponse
import retrofit2.Response
import retrofit2.http.GET

interface UserService {

    @GET("/api/v1/users/mypage")
    suspend fun getMyPage(): Response<ApiResponse<MyPageResponse>>
}
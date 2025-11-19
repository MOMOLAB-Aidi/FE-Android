package com.example.momolabfe.data.remote.user.service

import com.example.momolabfe.data.remote.user.data.MyPageResponse
import com.example.momolabfe.utils.ApiResponse
import retrofit2.Response
import retrofit2.http.GET

interface UserService {

    @GET("/api/v1/users/mypage")
    suspend fun getMyPage(): Response<ApiResponse<MyPageResponse>>
}
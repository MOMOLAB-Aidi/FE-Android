package com.example.momolabfe.data.remote.user.repository

import android.util.Log
import com.example.momolabfe.data.remote.user.data.MyPageResponse
import com.example.momolabfe.data.remote.user.service.UserService
import com.example.momolabfe.utils.handleApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor (
    private val userService: UserService,
){

    suspend fun getMyPage(): Result<MyPageResponse> = runCatching {
        val response = userService.getMyPage()
        Log.d("GetMyPage", "response = ${response.body()}")
        handleApiResponse(response)
    }
}
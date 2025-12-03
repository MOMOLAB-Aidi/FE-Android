package com.example.momolabfe.remote.user.repository

import android.util.Log
import com.example.momolabfe.remote.user.model.HospitalInfoResponse
import com.example.momolabfe.remote.user.model.MyPageResponse
import com.example.momolabfe.remote.user.model.UpdatePassword
import com.example.momolabfe.remote.user.service.UserService
import com.example.momolabfe.utils.handleApiResponse
import com.example.momolabfe.utils.handleApiResponseUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor (
    private val userService: UserService,
){

    suspend fun getMyPage(): Result<MyPageResponse> = runCatching {
        val response = userService.getMyPage()
        Log.d("GetMyPage", "MyPage API 호출 코드: ${response.code()}")
        handleApiResponse(response)
    }

    suspend fun updatePassword(request: UpdatePassword): Result<Unit> = runCatching {
        val response = userService.updatePassword(request)
        Log.d("UpdatePassword", "비밀번호 재설정 API 호출 코드: ${response.code()}")
        handleApiResponseUnit(response)
    }

    suspend fun getHospitalInfo(): Result<HospitalInfoResponse> = runCatching {
        val response = userService.getHospitalInfo()
        Log.d("GetHospitalInfo", "자주 가는 병원 조회 API 호출 코드: ${response.code()}")
        handleApiResponse(response)
    }
}
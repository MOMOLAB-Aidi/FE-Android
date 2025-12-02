package com.example.momolabfe.remote.fcm.repository

import android.util.Log
import com.example.momolabfe.remote.fcm.model.FcmTokenRequest
import com.example.momolabfe.remote.fcm.service.FcmService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepository @Inject constructor(
    private val fcmService: FcmService,
) {

    // 로그인/앱 실행 시 디바이스에서 받은 FCM 토큰을 서버에 등록
    suspend fun registerFcmToken(token: String): Result<Unit> = runCatching {
        val request = FcmTokenRequest(fcmToken = token)

        val response = fcmService.registerToken(request)
        Log.d("FcmRepository", "FCM 토큰 등록 API 호출 코드: ${response.code()}")

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            throw RuntimeException("FCM 토큰 등록 실패: code=${response.code()}, body=$errorBody")
        }

        Unit
    }

    // 로그아웃 시 서버에 해당 FCM 토큰 비활성화 요청
    suspend fun deactivateFcmToken(token: String): Result<Unit> = runCatching {
        val request = FcmTokenRequest(fcmToken = token)

        val response = fcmService.deactivateToken(request)
        Log.d("FcmRepository", "FCM 토큰 비활성화 API 호출 코드: ${response.code()}")

        if (!response.isSuccessful) {
            throw RuntimeException("FCM 토큰 비활성화 실패: code=${response.code()}")
        }

        Unit
    }
}
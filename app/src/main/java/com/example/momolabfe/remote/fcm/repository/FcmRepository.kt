package com.example.momolabfe.remote.fcm.repository

import com.example.momolabfe.remote.fcm.model.FcmTokenRequest
import com.example.momolabfe.remote.fcm.service.FcmService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRepository @Inject constructor(
    private val fcmService: FcmService,
) {

    private suspend fun callFcmApi(
        call: suspend () -> retrofit2.Response<Unit>,
        failMessage: String
    ): Result<Unit> = runCatching {
        val response = call()
        if (!response.isSuccessful) {
            throw RuntimeException("$failMessage: code=${response.code()}")
        }
        Unit
    }

    suspend fun registerFcmToken(token: String): Result<Unit> =
        callFcmApi({ fcmService.registerToken(FcmTokenRequest(fcmToken = token)) }, "FCM 토큰 등록 실패")

    suspend fun deactivateFcmToken(token: String): Result<Unit> =
        callFcmApi({ fcmService.deactivateToken(FcmTokenRequest(fcmToken = token)) }, "FCM 토큰 비활성화 실패")
}
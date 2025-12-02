package com.example.momolabfe.remote.education.repository

import android.util.Log
import com.example.momolabfe.remote.education.model.EducationResponse
import com.example.momolabfe.remote.education.service.EducationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EducationRepository @Inject constructor (
    private val educationService: EducationService,
) {

    suspend fun getTodayTip(): Result<EducationResponse> = runCatching {
        val response = educationService.getTodayTip()
        Log.d("GetTodayTip", "오늘의 복막투석 관리 TIP 조회 API 호출 코드: ${response.code()}")

        if (!response.isSuccessful) {
            throw RuntimeException("서버 오류: HTTP ${response.code()}")
        }

        response.body() ?: throw RuntimeException("응답이 비어 있습니다.")
    }
}
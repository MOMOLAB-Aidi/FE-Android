package com.example.momolabfe.remote.education.repository

import android.util.Log
import com.example.momolabfe.remote.education.model.EducationResponse
import com.example.momolabfe.remote.education.service.EducationService
import com.example.momolabfe.utils.handleApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EducationRepository @Inject constructor (
    private val educationService: EducationService,
) {

    suspend fun getTodayTip(): Result<EducationResponse> = runCatching {
        val response = educationService.getTodayTip()
        Log.d("GetTodayTip", "오늘의 복막투석 관리 TIP 조회 API 호출 코드: ${response.code()}")
        handleApiResponse(response)
    }
}
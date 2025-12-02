package com.example.momolabfe.remote.education.service

import com.example.momolabfe.remote.education.model.EducationResponse
import com.example.momolabfe.utils.ApiResponse
import retrofit2.Response
import retrofit2.http.GET

interface EducationService {
    @GET("/api/v1/education/today-tip")
    suspend fun getTodayTip(): Response<ApiResponse<EducationResponse>>
}
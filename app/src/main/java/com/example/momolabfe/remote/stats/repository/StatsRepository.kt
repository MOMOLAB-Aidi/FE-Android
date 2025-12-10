package com.example.momolabfe.remote.stats.repository

import com.example.momolabfe.remote.stats.model.Last7DaysAverageResponse
import com.example.momolabfe.remote.stats.model.Last7DaysStats
import com.example.momolabfe.remote.stats.service.StatsService
import com.example.momolabfe.utils.ApiException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val statsService: StatsService,
) {

    suspend fun getLast7Days(): Result<Last7DaysStats> = runCatching {
        val response = statsService.getLast7Days()
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "최근 7일간의 통계 조회 실패: HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    suspend fun getLast7DaysAverage(): Result<Last7DaysAverageResponse> = runCatching {
        val response = statsService.getLast7DaysAverage()
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "최근 7일간의 평균 통계 조회 실패: HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }
}
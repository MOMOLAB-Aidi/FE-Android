package com.example.momolabfe.remote.stats.service

import com.example.momolabfe.remote.stats.model.Last7DaysStats
import retrofit2.Response
import retrofit2.http.GET

interface StatsService {
    @GET("/api/v1/stats/last-7-days")
    suspend fun getLast7Days(): Response<Last7DaysStats>
}
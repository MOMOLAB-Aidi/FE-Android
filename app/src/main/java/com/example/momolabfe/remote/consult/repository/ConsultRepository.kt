package com.example.momolabfe.remote.consult.repository

import com.example.momolabfe.remote.consult.data.ChatRequest
import com.example.momolabfe.remote.consult.data.ChatResponse
import com.example.momolabfe.remote.consult.data.SessionEndRequest
import com.example.momolabfe.remote.consult.data.SessionEndResponse
import com.example.momolabfe.remote.consult.data.SessionStartResponse
import com.example.momolabfe.remote.consult.service.ConsultService
import com.example.momolabfe.utils.ApiException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsultRepository @Inject constructor(
    private val consultService: ConsultService,
) {

    // 새로운 복막투석 상담 시작
    suspend fun startConsult(): Result<SessionStartResponse> = runCatching {
        val response = consultService.startConsult()
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 에이전트 대화
    suspend fun chat(request: ChatRequest): Result<ChatResponse> = runCatching {
        val response = consultService.chat(request)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 상담 종료
    suspend fun endConsult(request: SessionEndRequest): Result<SessionEndResponse> = runCatching {
        val response = consultService.endConsult(request)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }
}
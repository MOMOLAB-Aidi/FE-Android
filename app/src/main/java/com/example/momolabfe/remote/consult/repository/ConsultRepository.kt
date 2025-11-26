package com.example.momolabfe.remote.consult.repository

import com.example.momolabfe.remote.consult.data.ChatRequest
import com.example.momolabfe.remote.consult.data.StartConsultResponse
import com.example.momolabfe.remote.consult.service.ConsultService
import com.example.momolabfe.utils.ApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsultRepository @Inject constructor(
    private val consultService: ConsultService,
) {

    // 상담 시작
    suspend fun startConsult(): Result<StartConsultResponse> = runCatching {
        val response = consultService.startConsult()
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "상담 시작 실패: HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 에이전트 대화
    fun chatStream(request: ChatRequest): Flow<String> = flow {
        val response = consultService.chatStream(request)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "에이전트 대화 실패: HTTP ${response.code()}")
        }
        val body: ResponseBody = response.body()
            ?: throw ApiException(
                response.code(),
                "에이전트 대화 실패: 응답 본문이 비어 있습니다."
            )

        body.use { responseBody ->
            val source = responseBody.source()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                emit(line) // 여기서 한 줄씩 Flow로 흘려보냄
            }
        }
    }
}
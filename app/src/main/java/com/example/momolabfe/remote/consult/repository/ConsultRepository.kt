package com.example.momolabfe.remote.consult.repository

import com.example.momolabfe.remote.consult.model.ChatRequest
import com.example.momolabfe.remote.consult.model.ConsultDetailResponse
import com.example.momolabfe.remote.consult.model.GetConsultResponse
import com.example.momolabfe.remote.consult.model.SessionEndRequest
import com.example.momolabfe.remote.consult.model.SessionEndResponse
import com.example.momolabfe.remote.consult.model.StartConsultResponse
import com.example.momolabfe.remote.consult.service.ConsultService
import com.example.momolabfe.utils.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    fun chatStream(request: ChatRequest): Flow<String> =
        flow {
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
                    // 백엔드에서 chunk + "\n" 으로 내려줌
                    val line = source.readUtf8Line() ?: break

                    emit(line)
                    kotlinx.coroutines.delay(100)
                }
            }
        }
        // 네트워크 + while 루프는 IO 스레드에서 돌리기
        .flowOn(Dispatchers.IO)

    // 상담 종료
    suspend fun endConsult(request: SessionEndRequest): Result<SessionEndResponse> = runCatching {
        val response = consultService.endConsult(request)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "상담 종료 실패: HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 전체 상담 기록 목록 조회
    suspend fun getConsultList(skip: Int, limit: Int): Result<List<GetConsultResponse>> = runCatching {
        val response = consultService.getConsultList(skip, limit)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "상담 기록 목록 조회 실패: HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 특정 상담 기록 상세 조회
    suspend fun getConsult(sessionId: String): Result<List<ConsultDetailResponse>> = runCatching {
        val response = consultService.getConsult(sessionId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "상담 기록 상세 조회 실패: HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 특정 세션 상담 기록 삭제
    suspend fun deleteConsult(sessionId: String): Result<Unit> = runCatching {
        val response = consultService.deleteConsult(sessionId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "특정 세션 상담 기록 삭제 실패: HTTP ${response.code()}")
        }
        Unit
    }
}
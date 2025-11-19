package com.example.momolabfe.remote.consult.service

import com.example.momolabfe.remote.consult.data.ChatRequest
import com.example.momolabfe.remote.consult.data.ChatResponse
import com.example.momolabfe.remote.consult.data.SessionEndRequest
import com.example.momolabfe.remote.consult.data.SessionEndResponse
import com.example.momolabfe.remote.consult.data.SessionStartResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ConsultService {

    // 새로운 복막투석 상담 시작
    @GET("/api/v1/consult/start")
    suspend fun startConsult(): Response<SessionStartResponse>

    // 에이전트 대화
    @POST("/api/v1/consult/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    // 상담 종료
    @POST("/api/v1/consult/end")
    suspend fun endConsult(@Body request: SessionEndRequest): Response<SessionEndResponse>
}
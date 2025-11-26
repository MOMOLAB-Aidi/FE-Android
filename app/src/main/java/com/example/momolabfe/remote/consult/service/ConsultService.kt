package com.example.momolabfe.remote.consult.service

import com.example.momolabfe.remote.consult.data.ChatRequest
import com.example.momolabfe.remote.consult.data.ConsultDetailResponse
import com.example.momolabfe.remote.consult.data.GetConsultResponse
import com.example.momolabfe.remote.consult.data.SessionEndRequest
import com.example.momolabfe.remote.consult.data.SessionEndResponse
import com.example.momolabfe.remote.consult.data.StartConsultResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ConsultService {

    @POST("/api/v1/consults/start")
    suspend fun startConsult(): Response<StartConsultResponse>

    @Streaming
    @POST("/api/v1/consults/chat")
    suspend fun chatStream(@Body request: ChatRequest): Response<ResponseBody>

    @POST("/api/v1/consults/end")
    suspend fun endConsult(@Body request: SessionEndRequest): Response<SessionEndResponse>

    @GET("/api/v1/consults/history")
    suspend fun getConsultList(@Query("skip") skip: Int, @Query("limit") limit: Int): Response<List<GetConsultResponse>>

    @GET("/api/v1/consults/history/{session_id}")
    suspend fun getConsult(@Path("session_id") sessionId: String): Response<ConsultDetailResponse>
}
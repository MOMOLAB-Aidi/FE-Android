package com.example.momolabfe.data.remote.record.service

import com.example.momolabfe.data.remote.record.model.GetCalendarResponse
import com.example.momolabfe.data.remote.record.model.RecordCreateRequest
import com.example.momolabfe.data.remote.record.model.RecordExchangeCreateRequest
import com.example.momolabfe.data.remote.record.model.RecordIdResponse
import com.example.momolabfe.data.remote.record.model.RecordOcrResponse
import com.example.momolabfe.utils.ApiResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface RecordService {

    @GET("/api/v1/records/calendar")
    suspend fun getCalendar(@Query("year") year: Int, @Query("month") month: Int): Response<ApiResponse<List<GetCalendarResponse>>>

    @POST("/api/v1/records")
    suspend fun recordCommonByWriting(@Body request: RecordCreateRequest): Response<RecordIdResponse>

    @POST("/api/v1/records/{rec_id}/exchanges")
    suspend fun recordExchangeByWriting(@Path("rec_id") recId: Long, @Body request: RecordExchangeCreateRequest): Response<Unit>

    @Multipart
    @POST("/api/v1/ocr")
    suspend fun recordByOcr(@Part file: MultipartBody.Part): Response<RecordOcrResponse>
}
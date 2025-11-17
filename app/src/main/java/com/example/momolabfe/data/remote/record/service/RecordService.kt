package com.example.momolabfe.data.remote.record.service

import com.example.momolabfe.data.remote.record.model.GetCalendarResponse
import com.example.momolabfe.data.remote.record.model.RecordCreateRequest
import com.example.momolabfe.data.remote.record.model.RecordExchangeCreateRequest
import com.example.momolabfe.data.remote.record.model.RecordIdResponse
import com.example.momolabfe.data.remote.record.model.RecordOcrResponse
import com.example.momolabfe.utils.ApiResponse
import com.example.momolabfe.utils.AuthRetrofit
import com.example.momolabfe.utils.PythonRetrofit
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

abstract class RecordService (
    @AuthRetrofit springRetrofit: Retrofit,
    @PythonRetrofit pythonRetrofit: Retrofit
) {

    private interface SpringApiService {
        @GET("/api/v1/records/calendar")
        suspend fun getCalendar(@Query("year") year: Int, @Query("month") month: Int): Response<ApiResponse<List<GetCalendarResponse>>>
    }

    private interface PythonApiService {
        @POST("/api/v1/records")
        suspend fun recordCommonByWriting(@Body request: RecordCreateRequest): Response<RecordIdResponse>

        @POST("/api/v1/records/{rec_id}/exchanges")
        suspend fun recordExchangeByWriting(@Path("rec_id") recId: Long, @Body request: RecordExchangeCreateRequest): Response<Unit>

        @Multipart
        @POST("/api/v1/ocr")
        suspend fun recordByOcr(@Part file: MultipartBody.Part): Response<RecordOcrResponse>
    }

    // 내부 서비스 인스턴스 초기화
    private val springService: SpringApiService = springRetrofit.create(SpringApiService::class.java)
    private val pythonService: PythonApiService = pythonRetrofit.create(PythonApiService::class.java)

    suspend fun getCalendar(year: Int, month: Int): Response<ApiResponse<List<GetCalendarResponse>>> {
        return springService.getCalendar(year, month)
    }

    suspend fun recordCommonByWriting(request: RecordCreateRequest): Response<RecordIdResponse> {
        return pythonService.recordCommonByWriting(request)
    }

    suspend fun recordExchangeByWriting(recId: Long, request: RecordExchangeCreateRequest): Response<Unit> {
        return pythonService.recordExchangeByWriting(recId, request)
    }

    suspend fun recordByOcr(file: MultipartBody.Part): Response<RecordOcrResponse> {
        return pythonService.recordByOcr(file)
    }
}
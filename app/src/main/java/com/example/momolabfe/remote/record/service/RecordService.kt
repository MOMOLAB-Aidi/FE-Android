package com.example.momolabfe.remote.record.service

import com.example.momolabfe.remote.record.model.GetCalendarResponse
import com.example.momolabfe.remote.record.model.RecordCreateRequest
import com.example.momolabfe.remote.record.model.RecordGetResponse
import com.example.momolabfe.remote.record.model.RecordOcrResponse
import com.example.momolabfe.remote.record.model.RecordUpdateRequest
import com.example.momolabfe.remote.record.model.WeeklyAverageResponse
import com.example.momolabfe.utils.ApiResponse
import com.example.momolabfe.utils.AuthRetrofit
import com.example.momolabfe.utils.PythonRetrofit
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordService @Inject constructor (
    @AuthRetrofit springRetrofit: Retrofit,
    @PythonRetrofit pythonRetrofit: Retrofit
) {

    private interface SpringApiService {
        // 캘린더 조회
        @GET("/api/v1/records/calendar")
        suspend fun getCalendar(@Query("year") year: Int, @Query("month") month: Int): Response<ApiResponse<List<GetCalendarResponse>>>
    }

    private interface PythonApiService {
        // 기록 작성
        @POST("/api/v1/records")
        suspend fun createRecord(@Body request: RecordCreateRequest): Response<Unit>

        // 기록 수정
        @PATCH("/api/v1/records/{rec_id}")
        suspend fun updateRecord(@Path("rec_id") recId: Long, @Body request: RecordUpdateRequest): Response<Unit>

        // 전체 기록 조회
        @GET("/api/v1/records")
        suspend fun getRecordList(@Query("year") year: Int, @Query("month") month: Int): Response<List<RecordGetResponse>>

        // 특정 기록 조회
        @GET("/api/v1/records/{rec_id}")
        suspend fun getRecord(@Path("rec_id") recId: Long): Response<RecordGetResponse>

        // 최근 3개 기록 조회
        @GET("/api/v1/records/latest")
        suspend fun getRecentRecords(): Response<List<RecordGetResponse>>

        // 주간 평균 기록 조회
        @GET("/api/v1/records/weekly-average")
        suspend fun getWeeklyAvgRecords(@Query("target_date") targetDate: String? = null): Response<WeeklyAverageResponse>

        // 특정 기록 삭제
        @DELETE("/api/v1/records/{rec_id}")
        suspend fun deleteRecord(@Path("rec_id") recId: Long): Response<Unit>

        // OCR 인식
        @Multipart
        @POST("/api/v1/records/ocr")
        suspend fun recordByOcr(@Part file: MultipartBody.Part): Response<RecordOcrResponse>

        // OCR 이미지 다운로드
        @GET("/api/v1/records/ocr/image")
        suspend fun downloadOcrImage(@Query("gcs_path") gcsPath: String): Response<ResponseBody>

    }

    // 내부 서비스 인스턴스 초기화
    private val springService: SpringApiService = springRetrofit.create(SpringApiService::class.java)
    private val pythonService: PythonApiService = pythonRetrofit.create(PythonApiService::class.java)

    suspend fun getCalendar(year: Int, month: Int): Response<ApiResponse<List<GetCalendarResponse>>> {
        return springService.getCalendar(year, month)
    }

    suspend fun createRecord(request: RecordCreateRequest): Response<Unit> {
        return pythonService.createRecord(request)
    }

    suspend fun updateRecord(recId: Long, request: RecordUpdateRequest): Response<Unit> {
        return pythonService.updateRecord(recId, request)
    }

    suspend fun getRecordList(year: Int, month: Int): Response<List<RecordGetResponse>> {
        return pythonService.getRecordList(year, month)
    }

    suspend fun getRecord(recId: Long): Response<RecordGetResponse> {
        return pythonService.getRecord(recId)
    }

    suspend fun getRecentRecords(): Response<List<RecordGetResponse>> {
        return pythonService.getRecentRecords()
    }

    suspend fun getWeeklyAvgRecords(targetDate: String? = null): Response<WeeklyAverageResponse> {
        return pythonService.getWeeklyAvgRecords(targetDate)
    }

    suspend fun deleteRecord(recId: Long): Response<Unit> {
        return pythonService.deleteRecord(recId)
    }

    suspend fun recordByOcr(file: MultipartBody.Part): Response<RecordOcrResponse> {
        return pythonService.recordByOcr(file)
    }

    suspend fun downloadOcrImage(gcsPath: String): Response<ResponseBody> {
        return pythonService.downloadOcrImage(gcsPath)
    }
}
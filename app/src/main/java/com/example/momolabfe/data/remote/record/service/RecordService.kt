package com.example.momolabfe.data.remote.record.service

import com.example.momolabfe.data.remote.record.model.RecordCreateRequest
import com.example.momolabfe.data.remote.record.model.RecordOcrResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RecordService {

    @POST("/api/v1/records")
    suspend fun recordCommonByWriting(@Body request: RecordCreateRequest): Response<Unit>

    @Multipart
    @POST("/api/v1/ocr")
    suspend fun recordByOcr(@Part file: MultipartBody.Part): Response<RecordOcrResponse>
}